package com.xia.yue.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.xia.yue.core.FindingClassifier;
import com.xia.yue.core.FindingType;
import com.xia.yue.core.ResponseSummary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class AuthzScanner {
    private final MontoyaApi api;
    private final Supplier<ScanConfig> configSupplier;
    private final ResultSink sink;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicLong sequence = new AtomicLong();

    public AuthzScanner(MontoyaApi api, Supplier<ScanConfig> configSupplier, ResultSink sink) {
        this.api = api;
        this.configSupplier = configSupplier;
        this.sink = sink;
    }

    public void submit(HttpRequest request) {
        HttpRequest stableRequest = request.copyToTempFile();
        executor.submit(() -> scan(stableRequest));
    }

    private void scan(HttpRequest originalRequest) {
        try {
            ScanConfig config = configSupplier.get();
            HttpRequest lowPrivilegeRequest = BurpRequestMutator.applyLowPrivilegeHeaders(originalRequest, config);
            HttpRequest unauthorizedRequest = BurpRequestMutator.removeUnauthorizedHeaders(originalRequest, config);

            HttpRequestResponse original = api.http().sendRequest(originalRequest);
            HttpRequestResponse lowPrivilege = api.http().sendRequest(lowPrivilegeRequest);
            HttpRequestResponse unauthorized = api.http().sendRequest(unauthorizedRequest);

            ResponseSummary originalSummary = BurpSummaries.summarize(original.response());
            ResponseSummary lowPrivilegeSummary = BurpSummaries.summarize(lowPrivilege.response());
            ResponseSummary unauthorizedSummary = BurpSummaries.summarize(unauthorized.response());
            FindingType type = FindingClassifier.classify(originalSummary, lowPrivilegeSummary, unauthorizedSummary);

            sink.accept(new ScanResult(
                    sequence.incrementAndGet(),
                    type,
                    safeUrl(originalRequest),
                    original,
                    lowPrivilege,
                    unauthorized,
                    originalSummary,
                    lowPrivilegeSummary,
                    unauthorizedSummary
            ));
        } catch (Exception e) {
            sink.error("越权检测失败", e);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static String safeUrl(HttpRequest request) {
        try {
            return request.url();
        } catch (Exception e) {
            return request.httpService() + request.path();
        }
    }

}
