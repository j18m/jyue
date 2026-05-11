package com.xia.yue.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainWhitelistTest {
    @Test
    void emptyWhitelistDoesNotAllowAnyHostWhenEnabled() {
        assertFalse(DomainWhitelist.parse("").allows("example.com"));
    }

    @Test
    void parsesMultipleHostSeparators() {
        DomainWhitelist whitelist = DomainWhitelist.parse("https://example.com:8443/path, api.test.local\nfoo.bar");

        assertTrue(whitelist.allows("example.com"));
        assertTrue(whitelist.allows("www.example.com"));
        assertTrue(whitelist.allows("https://www.example.com:9443/api/users"));
        assertTrue(whitelist.allows("api.test.local"));
        assertFalse(whitelist.allows("evil-example.com"));
        assertFalse(whitelist.allows("evil.test"));
    }

    @Test
    void supportsWildcardDomainSyntax() {
        DomainWhitelist whitelist = DomainWhitelist.parse("*.example.org, .internal.test");

        assertTrue(whitelist.allows("example.org"));
        assertTrue(whitelist.allows("api.example.org"));
        assertTrue(whitelist.allows("admin.internal.test"));
        assertFalse(whitelist.allows("fakeexample.org"));
    }

    @Test
    void supportsIpAddresses() {
        DomainWhitelist whitelist = DomainWhitelist.parse("127.0.0.1, 192.168.1.8:8080");

        assertTrue(whitelist.allows("127.0.0.1"));
        assertTrue(whitelist.allows("192.168.1.8"));
        assertFalse(whitelist.allows("192.168.1.9"));
    }
}
