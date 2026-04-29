package com.example.securitycontext.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /**
     * Shared HS256 secret for the demo. Use at least 32 chars.
     * In production use RS256 with separate signer/verifier keys.
     */
    private String secret = "demo-jwt-secret-please-change-in-production-32+chars";

    private String issuer = "auth-service";

    private Duration accessTokenTtl = Duration.ofMinutes(30);

    private Duration clockSkew = Duration.ofSeconds(5);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }
}
