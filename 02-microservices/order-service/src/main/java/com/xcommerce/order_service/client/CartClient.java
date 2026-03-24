package com.xcommerce.order_service.client;

import com.xcommerce.order_service.dto.CartItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "cart-service", url = "${cart.service.url:http://cart-service:8083/cart}")
public interface CartClient {
    @GetMapping
    List<CartItemDTO> getCartItems(@RequestHeader("X-User-Name") String username);

    @DeleteMapping
    void clearCart(@RequestHeader("X-User-Name") String username);
}
