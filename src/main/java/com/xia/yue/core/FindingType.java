package com.xia.yue.core;

public enum FindingType {
    LOW_PRIVILEGE_ACCESS("低权限可访问"),
    UNAUTHORIZED_ACCESS("未授权可访问"),
    SIMILAR_RESPONSE("疑似无差异"),
    NO_ISSUE("未发现"),
    FAILED("检测失败");

    private final String label;

    FindingType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
