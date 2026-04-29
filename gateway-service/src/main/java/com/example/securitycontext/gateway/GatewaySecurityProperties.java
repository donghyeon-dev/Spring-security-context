package com.example.securitycontext.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security")
public class GatewaySecurityProperties {

    /** jwt | passport */
    private String mode = "jwt";

    private Jwt jwt = new Jwt();
    private Passport passport = new Passport();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Passport getPassport() {
        return passport;
    }

    public void setPassport(Passport passport) {
        this.passport = passport;
    }

    public static class Jwt {
        private String secret = "demo-jwt-secret-please-change-in-production-32+chars";
        private String issuer = "auth-service";
        private Duration clockSkew = Duration.ofSeconds(5);

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Duration getClockSkew() { return clockSkew; }
        public void setClockSkew(Duration clockSkew) { this.clockSkew = clockSkew; }
    }

    public static class Passport {
        private String secret = "demo-passport-secret-please-change-internal-only-32+chars";
        private String issuer = "gateway-service";
        private Duration ttl = Duration.ofSeconds(30);
        private String headerName = "X-Passport";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
    }
}
