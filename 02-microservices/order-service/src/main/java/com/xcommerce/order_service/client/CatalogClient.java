package com.xcommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "catalog-service", url = "${catalog.service.url:http://catalog-service:8082/products}")
public interface CatalogClient {

    @GetMapping("/{id}/price")
    BigDecimal getProductPrice(@PathVariable("id") Long productId);
}
