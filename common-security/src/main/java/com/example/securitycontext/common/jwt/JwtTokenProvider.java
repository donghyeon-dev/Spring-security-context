package com.example.securitycontext.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(String tenantId, String userId, Collection<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .id(tokenId)
                .subject(userId)
                .claim("tenantId", tenantId)
                .claim("userId", userId)
                .claim("tokenId", tokenId)
                .claim("roles", roles == null ? List.of() : List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, tokenId, now, exp);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token);
    }

    public record IssuedToken(String token, String tokenId, Instant issuedAt, Instant expiresAt) {
    }
}
