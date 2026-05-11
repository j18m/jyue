package com.xia.yue.burp;

import com.xia.yue.core.FindingType;
import com.xia.yue.core.ResponseSummary;

public record ScanResult(
        long id,
        FindingType findingType,
        String url,
        CapturedExchange originalMessage,
        CapturedExchange lowPrivilegeMessage,
        CapturedExchange unauthorizedMessage,
        ResponseSummary originalSummary,
        ResponseSummary lowPrivilegeSummary,
        ResponseSummary unauthorizedSummary
) {
}
