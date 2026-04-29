package com.example.securitycontext.common.passport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PassportIssuer {

    private final PassportProperties properties;
    private final SecretKey signingKey;

    public PassportIssuer(PassportProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String tenantId,
                        String userId,
                        String originalTokenId,
                        Collection<String> roles,
                        String requestId) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getTtl());
        String rid = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("tenantId", tenantId)
                .claim("userId", userId)
                .claim("originalTokenId", originalTokenId)
                .claim("requestId", rid)
                .claim("roles", roles == null ? List.of() : List.copyOf(roles))
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
