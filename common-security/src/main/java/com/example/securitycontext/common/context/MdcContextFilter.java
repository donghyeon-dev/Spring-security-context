package com.example.securitycontext.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MdcContextFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            SecurityContextUtils.currentPrincipal().ifPresent(p -> {
                if (p.tenantId() != null) MDC.put(MdcKeys.TENANT_ID, p.tenantId());
                if (p.userId() != null) MDC.put(MdcKeys.USER_ID, p.userId());
                if (p.tokenId() != null) MDC.put(MdcKeys.TOKEN_ID, p.tokenId());
                if (p.requestId() != null) MDC.put(MdcKeys.REQUEST_ID, p.requestId());
                if (p.source() != null) MDC.put(MdcKeys.AUTH_SOURCE, p.source().name());
            });
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.TENANT_ID);
            MDC.remove(MdcKeys.USER_ID);
            MDC.remove(MdcKeys.TOKEN_ID);
            MDC.remove(MdcKeys.REQUEST_ID);
            MDC.remove(MdcKeys.AUTH_SOURCE);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
