package com.example.securitycontext.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationGlobalFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewaySecurityProperties props;
    private final SecretKey jwtKey;
    private final SecretKey passportKey;

    public AuthenticationGlobalFilter(GatewaySecurityProperties props) {
        this.props = props;
        this.jwtKey = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.passportKey = Keys.hmacShaKeyFor(props.getPassport().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();
        if (path.startsWith("/auth/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "missing Bearer token");
        }
        String token = auth.substring(BEARER_PREFIX.length());

        Claims c;
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(jwtKey)
                    .clockSkewSeconds(props.getJwt().getClockSkew().toSeconds())
                    .requireIssuer(props.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token);
            c = jws.getPayload();
        } catch (Exception ex) {
            log.warn("gateway JWT verification failed: {}", ex.getMessage());
            return unauthorized(exchange, "invalid JWT");
        }

        String requestId = req.getHeaders().getFirst("X-Request-Id");
        if (requestId == null) requestId = UUID.randomUUID().toString();

        ServerHttpRequest mutated;
        if ("passport".equalsIgnoreCase(props.getMode())) {
            String passport = issuePassport(c, requestId);
            String finalRequestId = requestId;
            mutated = req.mutate()
                    .headers(h -> {
                        h.remove(HttpHeaders.AUTHORIZATION);
                        h.set(props.getPassport().getHeaderName(), passport);
                        h.set("X-Request-Id", finalRequestId);
                    })
                    .build();
            log.info("issued passport tenantId={} userId={} jti={} requestId={}",
                    c.get("tenantId"), c.get("userId"), c.getId(), requestId);
        } else {
            String finalRequestId1 = requestId;
            mutated = req.mutate()
                    .headers(h -> h.set("X-Request-Id", finalRequestId1))
                    .build();
            log.info("forwarded JWT tenantId={} userId={} jti={} requestId={}",
                    c.get("tenantId"), c.get("userId"), c.getId(), requestId);
        }
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String issuePassport(Claims jwt, String requestId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getPassport().getTtl());
        @SuppressWarnings("unchecked")
        List<String> roles = jwt.get("roles", List.class);
        return Jwts.builder()
                .issuer(props.getPassport().getIssuer())
                .id(UUID.randomUUID().toString())
                .subject(jwt.get("userId", String.class))
                .claim("tenantId", jwt.get("tenantId", String.class))
                .claim("userId", jwt.get("userId", String.class))
                .claim("originalTokenId", jwt.getId())
                .claim("requestId", requestId)
                .claim("roles", roles == null ? List.of() : roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(passportKey, Jwts.SIG.HS256)
                .compact();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.warn("gateway 401: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
