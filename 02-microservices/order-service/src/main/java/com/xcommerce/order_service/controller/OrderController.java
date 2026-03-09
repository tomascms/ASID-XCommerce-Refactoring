package com.xcommerce.order_service.controller;

import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired private OrderService service;

    @PostMapping
    public Order placeOrder(@RequestHeader("X-User-Name") String username) {
        return service.createOrder(username);
    }
}