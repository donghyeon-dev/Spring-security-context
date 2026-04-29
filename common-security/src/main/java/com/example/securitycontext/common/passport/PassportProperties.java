package com.example.securitycontext.common.passport;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.passport")
public class PassportProperties {

    /**
     * Internal HS256 secret used by the gateway issuer and downstream verifiers.
     */
    private String secret = "demo-passport-secret-please-change-internal-only-32+chars";

    private String issuer = "gateway-service";

    private Duration ttl = Duration.ofSeconds(30);

    private Duration clockSkew = Duration.ofSeconds(5);

    private String headerName = "X-Passport";

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

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}
