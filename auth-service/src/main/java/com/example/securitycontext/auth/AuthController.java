package com.example.securitycontext.auth;

import com.example.securitycontext.common.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider tokenProvider;
    private final InMemoryUserDirectory users;

    public AuthController(JwtTokenProvider tokenProvider, InMemoryUserDirectory users) {
        this.tokenProvider = tokenProvider;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var maybeUser = users.find(request.tenantId(), request.userId());
        if (maybeUser.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "unknown user"));
        }
        var user = maybeUser.get();
        var issued = tokenProvider.issue(user.tenantId(), user.userId(), user.roles());
        log.info("issued JWT tenantId={} userId={} tokenId={}", user.tenantId(), user.userId(), issued.tokenId());
        return ResponseEntity.ok(new LoginResponse(
                issued.token(),
                issued.tokenId(),
                issued.issuedAt(),
                issued.expiresAt()));
    }

    public record LoginRequest(String tenantId, String userId) {
    }

    public record LoginResponse(String accessToken, String tokenId, Instant issuedAt, Instant expiresAt) {
    }

    public record DemoUser(String tenantId, String userId, Set<String> roles) {
    }
}
