package com.xia.yue.burp;

import burp.api.montoya.http.message.responses.HttpResponse;
import com.xia.yue.core.ResponseSummary;

public final class BurpSummaries {
    private static final int PREVIEW_LIMIT = 512;

    private BurpSummaries() {
    }

    public static ResponseSummary summarize(HttpResponse response) {
        if (response == null) {
            return null;
        }
        String body = response.bodyToString();
        String preview = body.length() > PREVIEW_LIMIT ? body.substring(0, PREVIEW_LIMIT) : body;
        String mimeType = response.mimeType() == null ? "" : response.mimeType().toString();
        return new ResponseSummary(response.statusCode(), response.toByteArray().length(), mimeType, preview);
    }
}
