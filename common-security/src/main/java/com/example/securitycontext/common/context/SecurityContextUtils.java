package com.example.securitycontext.common.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static Optional<TenantPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TenantPrincipal p)) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    public static TenantPrincipal requirePrincipal() {
        return currentPrincipal()
                .orElseThrow(() -> new IllegalStateException("No authenticated TenantPrincipal in SecurityContext"));
    }

    public static String currentUserId() {
        return currentPrincipal().map(TenantPrincipal::userId).orElse(null);
    }

    public static String currentTenantId() {
        return currentPrincipal().map(TenantPrincipal::tenantId).orElse(null);
    }

    public static String currentTokenId() {
        return currentPrincipal().map(TenantPrincipal::tokenId).orElse(null);
    }
}
