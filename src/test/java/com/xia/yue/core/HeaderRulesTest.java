package com.xia.yue.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderRulesTest {
    @Test
    void parsesLowPrivilegeHeaders() {
        assertEquals(
                List.of(new AuthHeader("Cookie", "a=b"), new AuthHeader("Authorization", "Bearer token")),
                HeaderRules.parseAuthHeaders("Cookie: a=b\nAuthorization: Bearer token\ninvalid")
        );
    }

    @Test
    void parsesRemovedHeadersCaseInsensitively() {
        assertEquals(
                Set.of("cookie", "authorization", "token"),
                HeaderRules.parseRemovedHeaderNames("Cookie\nAuthorization: ignored\nTOKEN")
        );
    }
}
