package com.xia.yue.ui;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import com.xia.yue.burp.CapturedExchange;
import com.xia.yue.compat.MontoyaCompat;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

final class MessagePairView extends JPanel {
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;

    MessagePairView(UserInterface userInterface) {
        super(new BorderLayout());
        requestEditor = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY);
        responseEditor = userInterface.createHttpResponseEditor(EditorOptions.READ_ONLY);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                requestEditor.uiComponent(),
                responseEditor.uiComponent()
        );
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
    }

    void setMessage(CapturedExchange message) {
        if (message == null) {
            clear();
            return;
        }

        requestEditor.setRequest(message.request());
        if (message.response() != null) {
            responseEditor.setResponse(message.response());
        } else {
            setBlankResponse();
        }
    }

    void clear() {
        HttpRequest request = MontoyaCompat.emptyRequest();
        if (request != null) {
            requestEditor.setRequest(request);
        }
        setBlankResponse();
    }

    private void setBlankResponse() {
        HttpResponse response = MontoyaCompat.emptyResponse();
        if (response != null) {
            responseEditor.setResponse(response);
        }
    }
}
