package com.xia.yue.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FindingClassifierTest {
    @Test
    void flagsUnauthorizedWhenAnonymousResponseLooksLikeOriginal() {
        ResponseSummary original = new ResponseSummary((short) 200, 1000, "JSON", "{}");
        ResponseSummary low = new ResponseSummary((short) 403, 120, "HTML", "forbidden");
        ResponseSummary anonymous = new ResponseSummary((short) 200, 980, "JSON", "{}");

        assertEquals(FindingType.UNAUTHORIZED_ACCESS, FindingClassifier.classify(original, low, anonymous));
    }

    @Test
    void flagsLowPrivilegeWhenLowPrivilegeResponseLooksLikeOriginal() {
        ResponseSummary original = new ResponseSummary((short) 200, 1000, "JSON", "{}");
        ResponseSummary low = new ResponseSummary((short) 200, 990, "JSON", "{}");
        ResponseSummary anonymous = new ResponseSummary((short) 401, 90, "HTML", "login");

        assertEquals(FindingType.LOW_PRIVILEGE_ACCESS, FindingClassifier.classify(original, low, anonymous));
    }
}
