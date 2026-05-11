package com.xia.yue.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DedupStore {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public boolean markIfNew(RequestFingerprint fingerprint) {
        return seen.add(fingerprint.key());
    }

    public void clear() {
        seen.clear();
    }

    public int size() {
        return seen.size();
    }
}
