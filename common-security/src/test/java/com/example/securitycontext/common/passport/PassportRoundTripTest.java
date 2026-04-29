package com.example.securitycontext.common.passport;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassportRoundTripTest {

    private PassportProperties props() {
        PassportProperties p = new PassportProperties();
        p.setSecret("test-passport-secret-must-be-32-or-more-chars");
        p.setIssuer("gateway-service-test");
        p.setTtl(Duration.ofSeconds(30));
        return p;
    }

    @Test
    void issuerVerifierRoundTrip() {
        PassportProperties props = props();
        PassportIssuer issuer = new PassportIssuer(props);
        PassportVerifier verifier = new PassportVerifier(props);

        String token = issuer.issue("tenant-A", "u-1001", "jti-123",
                List.of("USER"), "req-1");
        Passport p = verifier.verify(token);

        assertThat(p.tenantId()).isEqualTo("tenant-A");
        assertThat(p.userId()).isEqualTo("u-1001");
        assertThat(p.originalTokenId()).isEqualTo("jti-123");
        assertThat(p.requestId()).isEqualTo("req-1");
        assertThat(p.roles()).containsExactly("USER");
    }

    @Test
    void rejectsForgedPassport() {
        PassportProperties props = props();
        PassportIssuer issuer = new PassportIssuer(props);
        String token = issuer.issue("tenant-A", "u-1001", "jti-123",
                List.of("USER"), "req-1");

        PassportProperties other = props();
        other.setSecret("different-passport-secret-also-32-characters");
        PassportVerifier verifier = new PassportVerifier(other);

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
