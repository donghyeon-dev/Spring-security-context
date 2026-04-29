package com.example.securitycontext.common.tracing;

import com.example.securitycontext.common.context.TenantPrincipal;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

public class TracingSpanEnricher {

    private final Tracer tracer;

    public TracingSpanEnricher(Tracer tracer) {
        this.tracer = tracer;
    }

    public void enrich(TenantPrincipal principal) {
        if (tracer == null || principal == null) return;
        Span current = tracer.currentSpan();
        if (current == null) return;
        if (principal.tenantId() != null) current.tag("tenant.id", principal.tenantId());
        if (principal.userId() != null) current.tag("user.id", principal.userId());
        if (principal.tokenId() != null) current.tag("token.id", principal.tokenId());
        if (principal.requestId() != null) current.tag("request.id", principal.requestId());
        if (principal.source() != null) current.tag("auth.source", principal.source().name());
    }
}
