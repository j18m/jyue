package com.xia.yue.core;

import java.util.Locale;
import java.util.Objects;

public record RequestFingerprint(String scheme, String host, int port, String method, String path, String query, String body) {
    public RequestFingerprint {
        scheme = normalize(scheme);
        host = normalize(host);
        method = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        query = normalizeSegment(query);
        body = normalizeSegment(body);
        path = normalizePath(path);
    }

    public static RequestFingerprint of(boolean secure, String host, int port, String method, String pathWithoutQuery) {
        return new RequestFingerprint(secure ? "https" : "http", host, port, method, pathWithoutQuery, null, null);
    }

    public static RequestFingerprint ofWithQuery(boolean secure, String host, int port, String method, String pathWithQuery) {
        String path = pathWithQuery == null ? "" : pathWithQuery;
        int queryIndex = path.indexOf('?');
        String query = queryIndex >= 0 ? path.substring(queryIndex + 1) : "";
        String pathWithoutQuery = queryIndex >= 0 ? path.substring(0, queryIndex) : path;
        return new RequestFingerprint(secure ? "https" : "http", host, port, method, pathWithoutQuery, query, null);
    }

    public static RequestFingerprint ofWithBody(boolean secure, String host, int port, String method, String pathWithoutQuery, String body) {
        return new RequestFingerprint(secure ? "https" : "http", host, port, method, pathWithoutQuery, null, body);
    }

    public String key() {
        return String.join("|", scheme, host, String.valueOf(port), method, path);
    }

    public String keyWithQuery() {
        return String.join("|", scheme, host, String.valueOf(port), method, path, Objects.toString(query, ""));
    }

    public String keyWithBody() {
        return String.join("|", scheme, host, String.valueOf(port), method, path, Objects.toString(body, ""));
    }

    public String keyWithParams() {
        if (body != null && !body.isBlank()) {
            return keyWithBody();
        }
        return keyWithQuery();
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSegment(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizePath(String value) {
        String path = Objects.toString(value, "").trim();
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
