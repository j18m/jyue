package com.xia.yue.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DedupStoreTest {
    @Test
    void deduplicatesByDomainMethodAndPathIgnoringQuery() {
        DedupStore store = new DedupStore();

        assertTrue(store.markIfNew(RequestFingerprint.of(true, "Example.COM", 443, "get", "/api/user?id=1")));
        assertFalse(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "GET", "/api/user?id=2")));
        assertTrue(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "POST", "/api/user?id=2")));
        assertTrue(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "GET", "/api/order?id=2")));
    }

    @Test
    void clearResetsSeenRequests() {
        DedupStore store = new DedupStore();
        RequestFingerprint fingerprint = RequestFingerprint.of(false, "example.com", 80, "GET", "/api/user");

        assertTrue(store.markIfNew(fingerprint));
        assertFalse(store.markIfNew(fingerprint));
        store.clear();
        assertTrue(store.markIfNew(fingerprint));
    }
}
