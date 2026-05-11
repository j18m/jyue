package com.xia.yue.core;

public final class FindingClassifier {
    private FindingClassifier() {
    }

    public static FindingType classify(ResponseSummary original, ResponseSummary lowPrivilege, ResponseSummary unauthorized) {
        if (original == null || lowPrivilege == null || unauthorized == null) {
            return FindingType.FAILED;
        }
        if (looksEquivalent(original, unauthorized)) {
            return FindingType.UNAUTHORIZED_ACCESS;
        }
        if (looksEquivalent(original, lowPrivilege)) {
            return FindingType.LOW_PRIVILEGE_ACCESS;
        }
        if (sameStatusAndCloseLength(original, lowPrivilege) || sameStatusAndCloseLength(original, unauthorized)) {
            return FindingType.SIMILAR_RESPONSE;
        }
        return FindingType.NO_ISSUE;
    }

    private static boolean looksEquivalent(ResponseSummary left, ResponseSummary right) {
        return left.isSuccessLike()
                && right.isSuccessLike()
                && sameStatusAndCloseLength(left, right)
                && left.mimeType().equalsIgnoreCase(right.mimeType());
    }

    private static boolean sameStatusAndCloseLength(ResponseSummary left, ResponseSummary right) {
        if (left.statusCode() != right.statusCode()) {
            return false;
        }
        int baseline = Math.max(left.length(), 1);
        int delta = Math.abs(left.length() - right.length());
        return delta <= Math.max(64, baseline * 0.1);
    }
}
