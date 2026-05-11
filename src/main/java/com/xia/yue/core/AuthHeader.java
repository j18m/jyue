package com.xia.yue.core;

import java.util.Objects;

public record AuthHeader(String name, String value) {
    public AuthHeader {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = name.trim();
        value = value.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Header name must not be empty");
        }
    }
}
