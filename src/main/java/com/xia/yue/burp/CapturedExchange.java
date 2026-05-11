package com.xia.yue.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public record CapturedExchange(HttpRequest request, HttpResponse response) {
    public static CapturedExchange requestOnly(HttpRequest request) {
        return new CapturedExchange(request, null);
    }

    public static CapturedExchange from(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return null;
        }
        return new CapturedExchange(requestResponse.request(), safeResponse(requestResponse));
    }

    private static HttpResponse safeResponse(HttpRequestResponse requestResponse) {
        try {
            return requestResponse.response();
        } catch (Exception | LinkageError ignored) {
            return null;
        }
    }
}
