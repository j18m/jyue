package com.xia.yue.core;

import java.util.Locale;
import java.util.Objects;

public record RequestFingerprint(String scheme, String host, int port, String method, String path) {
    public RequestFingerprint {
        scheme = normalize(scheme);
        host = normalize(host);
        method = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        path = normalizePath(path);
    }

    public static RequestFingerprint of(boolean secure, String host, int port, String method, String pathWithoutQuery) {
        return new RequestFingerprint(secure ? "https" : "http", host, port, method, pathWithoutQuery);
    }

    public String key() {
        return String.join("|", scheme, host, String.valueOf(port), method, path);
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
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
