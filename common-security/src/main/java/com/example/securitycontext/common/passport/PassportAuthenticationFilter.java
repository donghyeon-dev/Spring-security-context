package com.example.securitycontext.common.passport;

import com.example.securitycontext.common.context.AuthSource;
import com.example.securitycontext.common.context.TenantPrincipal;
import com.example.securitycontext.common.tracing.TracingSpanEnricher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PassportAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PassportAuthenticationFilter.class);

    private final PassportVerifier verifier;
    private final PassportProperties properties;
    private final TracingSpanEnricher spanEnricher;

    public PassportAuthenticationFilter(PassportVerifier verifier,
                                        PassportProperties properties,
                                        TracingSpanEnricher spanEnricher) {
        this.verifier = verifier;
        this.properties = properties;
        this.spanEnricher = spanEnricher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(properties.getHeaderName());
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Passport p = verifier.verify(header);
            TenantPrincipal principal = new TenantPrincipal(
                    p.userId(), p.tenantId(), p.originalTokenId(), p.requestId(),
                    p.roles(), AuthSource.PASSPORT);

            var authorities = p.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(principal, header, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            spanEnricher.enrich(principal);
        } catch (Exception ex) {
            log.warn("Passport authentication failed: {}", ex.getMessage());
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
