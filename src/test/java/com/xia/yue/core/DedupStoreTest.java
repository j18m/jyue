package com.xia.yue.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DedupStoreTest {
    @Test
    void deduplicatesByDomainMethodAndPathIgnoringQuery() {
        DedupStore store = new DedupStore();

        assertTrue(store.markIfNew(RequestFingerprint.of(true, "Example.COM", 443, "get", "/api/user?id=1"), DedupMode.URL_ONLY));
        assertFalse(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "GET", "/api/user?id=2"), DedupMode.URL_ONLY));
        assertTrue(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "POST", "/api/user?id=2"), DedupMode.URL_ONLY));
        assertTrue(store.markIfNew(RequestFingerprint.of(true, "example.com", 443, "GET", "/api/order?id=2"), DedupMode.URL_ONLY));
    }

    @Test
    void clearResetsSeenRequests() {
        DedupStore store = new DedupStore();
        RequestFingerprint fingerprint = RequestFingerprint.of(false, "example.com", 80, "GET", "/api/user");

        assertTrue(store.markIfNew(fingerprint, DedupMode.URL_ONLY));
        assertFalse(store.markIfNew(fingerprint, DedupMode.URL_ONLY));
        store.clear();
        assertTrue(store.markIfNew(fingerprint, DedupMode.URL_ONLY));
    }

    @Test
    void urlWithParamsModeDedupByQueryString() {
        DedupStore store = new DedupStore();

        assertTrue(store.markIfNew(RequestFingerprint.ofWithQuery(true, "example.com", 443, "GET", "/api/user?id=1"), DedupMode.URL_WITH_QUERY_OR_BODY));
        assertTrue(store.markIfNew(RequestFingerprint.ofWithQuery(true, "example.com", 443, "GET", "/api/user?id=2"), DedupMode.URL_WITH_QUERY_OR_BODY));
        assertFalse(store.markIfNew(RequestFingerprint.ofWithQuery(true, "example.com", 443, "GET", "/api/user?id=2"), DedupMode.URL_WITH_QUERY_OR_BODY));
    }

    @Test
    void urlWithParamsModeDedupByBody() {
        DedupStore store = new DedupStore();

        assertTrue(store.markIfNew(RequestFingerprint.ofWithBody(true, "example.com", 443, "POST", "/api/user", "name=alice"), DedupMode.URL_WITH_QUERY_OR_BODY));
        assertTrue(store.markIfNew(RequestFingerprint.ofWithBody(true, "example.com", 443, "POST", "/api/user", "name=bob"), DedupMode.URL_WITH_QUERY_OR_BODY));
        assertFalse(store.markIfNew(RequestFingerprint.ofWithBody(true, "example.com", 443, "POST", "/api/user", "name=bob"), DedupMode.URL_WITH_QUERY_OR_BODY));
    }

    @Test
    void disabledModeNeverDedup() {
        DedupStore store = new DedupStore();
        RequestFingerprint fp = RequestFingerprint.of(true, "example.com", 443, "GET", "/api/user");

        assertTrue(store.markIfNew(fp, DedupMode.DISABLED));
        assertTrue(store.markIfNew(fp, DedupMode.DISABLED));
        assertTrue(store.markIfNew(fp, DedupMode.DISABLED));
    }
}
