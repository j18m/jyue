package com.xia.yue.burp;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.xia.yue.core.AuthHeader;

import java.util.Locale;
import java.util.Set;

public final class BurpRequestMutator {
    private BurpRequestMutator() {
    }

    public static HttpRequest applyLowPrivilegeHeaders(HttpRequest request, ScanConfig config) {
        HttpRequest mutated = request;
        for (AuthHeader header : config.lowPrivilegeHeaders()) {
            mutated = mutated.withHeader(header.name(), header.value());
        }
        return mutated;
    }

    public static HttpRequest removeUnauthorizedHeaders(HttpRequest request, ScanConfig config) {
        HttpRequest mutated = request;
        Set<String> removed = config.unauthorizedRemovedHeaders();
        for (HttpHeader header : request.headers()) {
            if (removed.contains(header.name().toLowerCase(Locale.ROOT))) {
                mutated = mutated.withRemovedHeader(header.name());
            }
        }
        return mutated;
    }
}
