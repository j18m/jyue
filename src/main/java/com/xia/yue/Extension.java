package com.xia.yue;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.xia.yue.burp.AuthzScanner;
import com.xia.yue.burp.ScanConfig;
import com.xia.yue.core.DedupStore;
import com.xia.yue.core.RequestFingerprint;
import com.xia.yue.ui.XiaYuePanel;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public final class Extension implements BurpExtension {
    private final DedupStore dedupStore = new DedupStore();
    private XiaYuePanel panel;
    private AuthzScanner scanner;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("jyue");

        panel = new XiaYuePanel(api.userInterface(), dedupStore);
        scanner = new AuthzScanner(api, panel::currentConfig, panel);

        api.userInterface().registerSuiteTab("jyue", panel);
        api.http().registerHttpHandler(new AutoProxyHandler());
        api.userInterface().registerContextMenuItemsProvider(new SendToAuthzMenuProvider());
        api.extension().registerUnloadingHandler(scanner::shutdown);
        api.logging().logToOutput("jyue 越权检测插件已加载");
    }

    private final class AutoProxyHandler implements HttpHandler {
        @Override
        public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        @Override
        public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
            ScanConfig config = panel.currentConfig();
            HttpRequest request = responseReceived.initiatingRequest();
            if (shouldAutoScan(responseReceived.toolSource(), request, config)) {
                scanner.submit(request);
            }
            return ResponseReceivedAction.continueWith(responseReceived);
        }
    }

    private boolean shouldAutoScan(burp.api.montoya.core.ToolSource toolSource, HttpRequest request, ScanConfig config) {
        if (!config.enabled() || request == null) {
            return false;
        }
        if (toolSource.isFromTool(ToolType.EXTENSIONS) || !toolSource.isFromTool(ToolType.PROXY)) {
            return false;
        }
        if (!isSupportedAutoMethod(request.method()) || isStaticResource(request.pathWithoutQuery())) {
            return false;
        }

        HttpService service = request.httpService();
        if (config.whitelistEnabled() && !config.whitelist().allows(service.host())) {
            return false;
        }

        RequestFingerprint fingerprint = RequestFingerprint.of(
                service.secure(),
                service.host(),
                service.port(),
                request.method(),
                request.pathWithoutQuery()
        );
        return dedupStore.markIfNew(fingerprint);
    }

    private static boolean isSupportedAutoMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method);
    }

    private static boolean isStaticResource(String path) {
        String lower = path == null ? "" : path.toLowerCase();
        return lower.matches(".*\\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|mp4|mp3|avi|mov|map)$");
    }

    private final class SendToAuthzMenuProvider implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<HttpRequest> requests = collectRequests(event);
            if (requests.isEmpty()) {
                return List.of();
            }

            JMenuItem item = new JMenuItem("Send to jyue 越权检测");
            item.addActionListener(action -> requests.forEach(scanner::submit));
            return List.of(item);
        }

        private List<HttpRequest> collectRequests(ContextMenuEvent event) {
            List<HttpRequest> requests = new ArrayList<>();
            for (HttpRequestResponse requestResponse : event.selectedRequestResponses()) {
                if (requestResponse != null && requestResponse.request() != null) {
                    requests.add(requestResponse.request());
                }
            }
            event.messageEditorRequestResponse()
                    .map(messageEditor -> messageEditor.requestResponse().request())
                    .ifPresent(requests::add);
            return requests;
        }
    }
}
