package com.xia.yue.core;

import java.util.Locale;

public enum DedupMode {
    URL_ONLY("URL(默认)"),
    URL_WITH_QUERY_OR_BODY("请求参数"),
    DISABLED("不过滤");

    private final String label;

    DedupMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static DedupMode fromLabel(String label) {
        if (label == null) {
            return URL_ONLY;
        }
        String normalized = label.trim().toLowerCase(Locale.ROOT);
        for (DedupMode mode : values()) {
            if (mode.label.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return URL_ONLY;
    }
}
