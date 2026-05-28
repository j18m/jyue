package com.xia.yue.burp;

import com.xia.yue.core.AuthHeader;
import com.xia.yue.core.DedupMode;
import com.xia.yue.core.DomainWhitelist;

import java.util.List;
import java.util.Set;

public record ScanConfig(
        boolean enabled,
        boolean whitelistEnabled,
        DomainWhitelist whitelist,
        List<AuthHeader> lowPrivilegeHeaders,
        Set<String> unauthorizedRemovedHeaders,
        DedupMode dedupMode
) {
}
