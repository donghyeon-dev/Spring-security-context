package com.example.securitycontext.order;

import com.example.securitycontext.common.context.SecurityContextUtils;
import com.example.securitycontext.common.context.TenantPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final InventoryClient inventoryClient;
    private final AsyncWorker asyncWorker;

    public OrderController(InventoryClient inventoryClient, AsyncWorker asyncWorker) {
        this.inventoryClient = inventoryClient;
        this.asyncWorker = asyncWorker;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        TenantPrincipal p = SecurityContextUtils.requirePrincipal();
        log.info("/orders/me called");
        return principalView(p);
    }

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        TenantPrincipal p = SecurityContextUtils.requirePrincipal();
        String orderId = "ord-" + UUID.randomUUID();
        log.info("creating order {} sku={} qty={}", orderId, body.get("sku"), body.get("qty"));

        Map<String, Object> reserveReq = Map.of(
                "orderId", orderId,
                "sku", body.getOrDefault("sku", "A1"),
                "qty", body.getOrDefault("qty", 1)
        );
        Map<String, Object> reserveResp = inventoryClient.reserve(reserveReq);
        log.info("inventory replied: {}", reserveResp);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", orderId);
        resp.put("principal", principalView(p));
        resp.put("inventory", reserveResp);
        return resp;
    }

    @GetMapping("/async-context-check")
    public Map<String, Object> asyncContextCheck() throws Exception {
        log.info("dispatching async work");
        Map<String, Object> async = asyncWorker.snapshot().get();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("callerThread", Thread.currentThread().getName());
        resp.put("workerSnapshot", async);
        return resp;
    }

    private Map<String, Object> principalView(TenantPrincipal p) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("tenantId", p.tenantId());
        v.put("userId", p.userId());
        v.put("tokenId", p.tokenId());
        v.put("requestId", p.requestId());
        v.put("roles", p.roles());
        v.put("authSource", p.source());
        v.put("service", "order-service");
        return v;
    }
}
