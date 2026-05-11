package com.xia.yue.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class DomainWhitelist {
    private final Set<String> hosts;

    private DomainWhitelist(Set<String> hosts) {
        this.hosts = hosts;
    }

    public static DomainWhitelist parse(String text) {
        if (text == null || text.isBlank()) {
            return new DomainWhitelist(Set.of());
        }

        Set<String> hosts = Arrays.stream(text.split("[,，\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(DomainWhitelist::normalizeHost)
                .collect(Collectors.toUnmodifiableSet());
        return new DomainWhitelist(hosts);
    }

    public boolean allows(String host) {
        if (hosts.isEmpty()) {
            return false;
        }
        String normalized = normalizeHost(host);
        return hosts.stream().anyMatch(allowed -> matches(normalized, allowed));
    }

    private static String normalizeHost(String host) {
        String normalized = Objects.toString(host, "").trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^https?://", "");
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        if (normalized.startsWith("*.")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(0, colon);
        }
        return normalized;
    }

    private static boolean matches(String host, String allowed) {
        if (host.isEmpty() || allowed.isEmpty()) {
            return false;
        }
        return host.equals(allowed) || host.endsWith("." + allowed);
    }
}
