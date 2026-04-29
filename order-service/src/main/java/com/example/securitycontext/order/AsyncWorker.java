package com.example.securitycontext.order;

import com.example.securitycontext.common.context.SecurityContextUtils;
import com.example.securitycontext.common.context.TenantPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncWorker {

    private static final Logger log = LoggerFactory.getLogger(AsyncWorker.class);

    @Async
    public CompletableFuture<Map<String, Object>> snapshot() {
        TenantPrincipal p = SecurityContextUtils.currentPrincipal().orElse(null);
        log.info("async worker thread sees principal={}", p);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("workerThread", Thread.currentThread().getName());
        snap.put("tenantId", SecurityContextUtils.currentTenantId());
        snap.put("userId", SecurityContextUtils.currentUserId());
        snap.put("tokenId", SecurityContextUtils.currentTokenId());
        return CompletableFuture.completedFuture(snap);
    }
}
