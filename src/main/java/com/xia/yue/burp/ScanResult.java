package com.xia.yue.burp;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.xia.yue.core.FindingType;
import com.xia.yue.core.ResponseSummary;

public record ScanResult(
        long id,
        FindingType findingType,
        String url,
        HttpRequestResponse originalMessage,
        HttpRequestResponse lowPrivilegeMessage,
        HttpRequestResponse unauthorizedMessage,
        ResponseSummary originalSummary,
        ResponseSummary lowPrivilegeSummary,
        ResponseSummary unauthorizedSummary
) {
}
