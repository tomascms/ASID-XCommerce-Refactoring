package com.xcommerce.order_service.controller;

import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.repository.OrderRepository;
import com.xcommerce.order_service.service.OrderProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order savedOrder = repository.save(order);

        orderProducer.sendOrderEvents(
            savedOrder.getUsername(), 
            savedOrder.getProductId(), 
            savedOrder.getQuantity()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}