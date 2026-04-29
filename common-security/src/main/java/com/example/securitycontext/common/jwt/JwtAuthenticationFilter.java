package com.example.securitycontext.common.jwt;

import com.example.securitycontext.common.context.AuthSource;
import com.example.securitycontext.common.context.TenantPrincipal;
import com.example.securitycontext.common.tracing.TracingSpanEnricher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TracingSpanEnricher spanEnricher;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TracingSpanEnricher spanEnricher) {
        this.tokenProvider = tokenProvider;
        this.spanEnricher = spanEnricher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            Jws<Claims> jws = tokenProvider.parse(token);
            Claims c = jws.getPayload();
            String tenantId = c.get("tenantId", String.class);
            String userId = c.get("userId", String.class);
            String tokenId = c.get("tokenId", String.class);
            @SuppressWarnings("unchecked")
            List<String> rolesList = c.get("roles", List.class);
            Set<String> roles = rolesList == null ? Set.of() : new HashSet<>(rolesList);

            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null) requestId = UUID.randomUUID().toString();

            TenantPrincipal principal = new TenantPrincipal(
                    userId, tenantId, tokenId, requestId, roles, AuthSource.JWT);

            var authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            spanEnricher.enrich(principal);
        } catch (Exception ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
