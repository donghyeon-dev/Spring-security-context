package com.example.securitycontext.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "inventory-service", url = "${app.inventory.url:http://localhost:8083}")
public interface InventoryClient {

    @PostMapping("/inventory/reserve")
    Map<String, Object> reserve(@RequestBody Map<String, Object> request);
}
