package com.example.securitycontext.common.context;

import java.util.Set;

public record TenantPrincipal(
        String userId,
        String tenantId,
        String tokenId,
        String requestId,
        Set<String> roles,
        AuthSource source
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
