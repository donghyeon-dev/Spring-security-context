package com.example.securitycontext.inventory;

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

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping("/me")
    public Map<String, Object> me() {
        TenantPrincipal p = SecurityContextUtils.requirePrincipal();
        return view(p, null);
    }

    @PostMapping("/reserve")
    public Map<String, Object> reserve(@RequestBody Map<String, Object> body) {
        TenantPrincipal p = SecurityContextUtils.requirePrincipal();
        log.info("reserving sku={} qty={} for orderId={}",
                body.get("sku"), body.get("qty"), body.get("orderId"));
        Map<String, Object> resp = view(p, body);
        resp.put("status", "RESERVED");
        return resp;
    }

    private Map<String, Object> view(TenantPrincipal p, Map<String, Object> echoed) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("tenantId", p.tenantId());
        v.put("userId", p.userId());
        v.put("tokenId", p.tokenId());
        v.put("requestId", p.requestId());
        v.put("roles", p.roles());
        v.put("authSource", p.source());
        v.put("service", "inventory-service");
        if (echoed != null) v.put("request", echoed);
        return v;
    }
}
