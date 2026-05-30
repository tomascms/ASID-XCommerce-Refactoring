package com.xcommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "${inventory.service.url:http://inventory-service:8085/inventory}")
public interface InventoryClient {

    @GetMapping("/check")
    boolean checkStock(@RequestParam("productId") Long productId, @RequestParam("quantity") Integer quantity);

    @PostMapping("/decrease")
    void decreaseStock(@RequestParam("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
