package com.example.securitycontext.common.passport;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PassportVerifier {

    private final PassportProperties properties;
    private final SecretKey signingKey;

    public PassportVerifier(PassportProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Passport verify(String passportToken) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(passportToken);
        Claims c = jws.getPayload();
        @SuppressWarnings("unchecked")
        List<String> rolesList = c.get("roles", List.class);
        Set<String> roles = rolesList == null ? Set.of() : new HashSet<>(rolesList);
        return new Passport(
                c.get("tenantId", String.class),
                c.get("userId", String.class),
                c.get("originalTokenId", String.class),
                c.get("requestId", String.class),
                roles,
                c.getIssuedAt().toInstant(),
                c.getExpiration().toInstant()
        );
    }
}
