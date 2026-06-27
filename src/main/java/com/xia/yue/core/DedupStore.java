package com.xia.yue.core;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DedupStore {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public boolean markIfNew(RequestFingerprint fingerprint, DedupMode mode) {
        if (mode == null) {
            mode = DedupMode.URL_ONLY;
        }
        return switch (mode) {
            case DISABLED -> true;
            case URL_WITH_QUERY_OR_BODY -> seen.add(fingerprint.keyWithParams());
            default -> seen.add(fingerprint.key());
        };
    }

    public void clear() {
        seen.clear();
    }

    public int size() {
        return seen.size();
    }
}
