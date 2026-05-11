package com.xia.yue.core;

public record ResponseSummary(short statusCode, int length, String mimeType, String bodyPreview) {
    public boolean isSuccessLike() {
        return statusCode >= 200 && statusCode < 400;
    }
}
