package com.example.securitycontext.auth;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class InMemoryUserDirectory {

    private final List<AuthController.DemoUser> users = List.of(
            new AuthController.DemoUser("tenant-A", "u-1001", Set.of("USER")),
            new AuthController.DemoUser("tenant-A", "u-1002", Set.of("USER", "ADMIN")),
            new AuthController.DemoUser("tenant-B", "u-2001", Set.of("USER"))
    );

    public Optional<AuthController.DemoUser> find(String tenantId, String userId) {
        return users.stream()
                .filter(u -> u.tenantId().equals(tenantId) && u.userId().equals(userId))
                .findFirst();
    }
}
