package com.example.securitycontext.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final JwtProperties props = props();

    private JwtProperties props() {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-must-be-at-least-32-characters-long");
        p.setIssuer("auth-service-test");
        return p;
    }

    @Test
    void issuesAndParsesToken() {
        JwtTokenProvider provider = new JwtTokenProvider(props);

        var issued = provider.issue("tenant-A", "u-1001", List.of("USER", "ADMIN"));
        Jws<Claims> jws = provider.parse(issued.token());

        Claims c = jws.getPayload();
        assertThat(c.get("tenantId")).isEqualTo("tenant-A");
        assertThat(c.get("userId")).isEqualTo("u-1001");
        assertThat(c.get("tokenId")).isEqualTo(issued.tokenId());
        assertThat(c.get("roles", List.class)).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtTokenProvider issuer = new JwtTokenProvider(props);
        var issued = issuer.issue("tenant-A", "u-1001", List.of("USER"));

        JwtProperties other = new JwtProperties();
        other.setSecret("different-secret-also-32-characters-long-xx");
        other.setIssuer("auth-service-test");
        JwtTokenProvider verifier = new JwtTokenProvider(other);

        assertThatThrownBy(() -> verifier.parse(issued.token()))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
