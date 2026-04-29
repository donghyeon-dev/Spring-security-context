package com.example.securitycontext.common.passport;

import java.time.Instant;
import java.util.Set;

public record Passport(
        String tenantId,
        String userId,
        String originalTokenId,
        String requestId,
        Set<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}
