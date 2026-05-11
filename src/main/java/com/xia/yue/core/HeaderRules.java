package com.xia.yue.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class HeaderRules {
    private HeaderRules() {
    }

    public static List<AuthHeader> parseAuthHeaders(String text) {
        List<AuthHeader> headers = new ArrayList<>();
        for (String line : lines(text)) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (!name.isEmpty()) {
                headers.add(new AuthHeader(name, value));
            }
        }
        return headers;
    }

    public static Set<String> parseRemovedHeaderNames(String text) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : lines(text)) {
            String name = line;
            int colon = line.indexOf(':');
            if (colon > 0) {
                name = line.substring(0, colon);
            }
            name = name.trim();
            if (!name.isEmpty()) {
                names.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
