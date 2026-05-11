package com.xia.yue.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.xia.yue.compat.MontoyaCompat;
import com.xia.yue.core.FindingClassifier;
import com.xia.yue.core.FindingType;
import com.xia.yue.core.ResponseSummary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class AuthzScanner {
    private static final long RESPONSE_TIMEOUT_MILLIS = 10_000;
    private static final long SCAN_TIMEOUT_MILLIS = 35_000;
    private final MontoyaApi api;
    private final Supplier<ScanConfig> configSupplier;
    private final ResultSink sink;
    private final BiConsumer<String, Throwable> errorLogger;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ExecutorService requestExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong sequence = new AtomicLong();

    public AuthzScanner(MontoyaApi api, Supplier<ScanConfig> configSupplier, ResultSink sink, BiConsumer<String, Throwable> errorLogger) {
        this.api = api;
        this.configSupplier = configSupplier;
        this.sink = sink;
        this.errorLogger = errorLogger;
    }

    public void submit(HttpRequest request) {
        submit(CapturedExchange.requestOnly(request));
    }

    public void submit(HttpRequestResponse seedMessage) {
        submit(CapturedExchange.from(seedMessage));
    }

    public void submit(CapturedExchange seedMessage) {
        HttpRequest request = seedMessage.request();
        long id = sequence.incrementAndGet();
        sink.accept(new ScanResult(
                id,
                FindingType.QUEUED,
                safeUrl(request),
                seedMessage,
                null,
                null,
                BurpSummaries.summarize(seedMessage.response()),
                null,
                null
        ));

        AtomicBoolean finished = new AtomicBoolean(false);
        timeoutExecutor.schedule(() -> {
            if (finished.compareAndSet(false, true)) {
                sink.accept(failedResult(id, request, "越权检测超时，已超过 " + (SCAN_TIMEOUT_MILLIS / 1000) + " 秒"));
            }
        }, SCAN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        try {
            executor.submit(() -> {
                try {
                    scan(id, seedMessage, finished);
                } catch (Exception e) {
                    if (finished.compareAndSet(false, true)) {
                        logError("越权检测提交失败", e);
                        sink.accept(failedResult(id, request, "越权检测提交失败: " + e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            if (finished.compareAndSet(false, true)) {
                logError("越权检测提交失败", e);
                sink.accept(failedResult(id, request, "越权检测提交失败: " + e.getMessage()));
            }
        }
    }

    private void scan(long id, CapturedExchange seedMessage, AtomicBoolean finished) {
        HttpRequest originalRequest = seedMessage.request();
        try {
            ScanConfig config = configSupplier.get();
            HttpRequest lowPrivilegeRequest = BurpRequestMutator.applyLowPrivilegeHeaders(originalRequest, config);
            HttpRequest unauthorizedRequest = BurpRequestMutator.removeUnauthorizedHeaders(originalRequest, config);
            CapturedExchange original = seedMessage;
            CapturedExchange lowPrivilege = CapturedExchange.requestOnly(lowPrivilegeRequest);
            CapturedExchange unauthorized = CapturedExchange.requestOnly(unauthorizedRequest);
            ResponseSummary originalSummary = BurpSummaries.summarize(original.response());

            publishProgress(id, originalRequest, original, lowPrivilege, unauthorized, originalSummary, null, null, finished);

            lowPrivilege = CapturedExchange.from(sendWithDeadline(lowPrivilegeRequest));
            ResponseSummary lowPrivilegeSummary = BurpSummaries.summarize(lowPrivilege.response());
            publishProgress(id, originalRequest, original, lowPrivilege, unauthorized, originalSummary, lowPrivilegeSummary, null, finished);

            unauthorized = CapturedExchange.from(sendWithDeadline(unauthorizedRequest));
            ResponseSummary unauthorizedSummary = BurpSummaries.summarize(unauthorized.response());
            FindingType type = FindingClassifier.classify(originalSummary, lowPrivilegeSummary, unauthorizedSummary);

            if (finished.compareAndSet(false, true)) {
                sink.accept(new ScanResult(
                        id,
                        type,
                        safeUrl(originalRequest),
                        original,
                        lowPrivilege,
                        unauthorized,
                        originalSummary,
                        lowPrivilegeSummary,
                        unauthorizedSummary
                ));
            }
        } catch (Exception e) {
            if (finished.compareAndSet(false, true)) {
                logError("越权检测失败", e);
                sink.accept(failedResult(id, originalRequest, "越权检测失败: " + e.getMessage()));
            }
        }
    }

    private void logError(String message, Throwable throwable) {
        if (errorLogger != null) {
            errorLogger.accept(message, throwable);
        }
    }

    private void publishProgress(
            long id,
            HttpRequest originalRequest,
            CapturedExchange original,
            CapturedExchange lowPrivilege,
            CapturedExchange unauthorized,
            ResponseSummary originalSummary,
            ResponseSummary lowPrivilegeSummary,
            ResponseSummary unauthorizedSummary,
            AtomicBoolean finished
    ) {
        if (!finished.get()) {
            sink.accept(new ScanResult(
                    id,
                    FindingType.PENDING,
                    safeUrl(originalRequest),
                    original,
                    lowPrivilege,
                    unauthorized,
                    originalSummary,
                    lowPrivilegeSummary,
                    unauthorizedSummary
            ));
        }
    }

    public void shutdown() {
        executor.shutdownNow();
        requestExecutor.shutdownNow();
        timeoutExecutor.shutdownNow();
    }

    private HttpRequestResponse sendWithDeadline(HttpRequest request) throws Exception {
        Future<HttpRequestResponse> future = requestExecutor.submit(() -> MontoyaCompat.sendRequest(api, request, RESPONSE_TIMEOUT_MILLIS));
        try {
            return future.get(RESPONSE_TIMEOUT_MILLIS + 1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("请求响应超时，超过 " + (RESPONSE_TIMEOUT_MILLIS / 1000) + " 秒: " + safeUrl(request));
        }
    }

    private static String safeUrl(HttpRequest request) {
        try {
            return request.url();
        } catch (Exception e) {
            return request.httpService() + request.path();
        }
    }

    private static ScanResult failedResult(long id, HttpRequest request, String message) {
        return new ScanResult(
                id,
                FindingType.FAILED,
                message + " | " + safeUrl(request),
                CapturedExchange.requestOnly(request),
                null,
                null,
                null,
                null,
                null
        );
    }
}
