package com.xcommerce.order_service.controller;

import com.xcommerce.order_service.dto.CreateOrderRequest;
import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.dto.OrderResponse;
import com.xcommerce.order_service.model.OrderStatus;
import com.xcommerce.order_service.repository.OrderRepository;
import com.xcommerce.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest order,
        @RequestHeader(value = "X-User-Name", required = false) String username
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(orderService.createDirectOrder(username, order)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(orderService.createOrder(username)));
    }

    @GetMapping("/checkout")
    public ResponseEntity<OrderResponse> checkoutLegacy(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(orderService.createOrder(username)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
        @PathVariable @NonNull Long id,
        @RequestHeader("X-User-Name") String username,
        @RequestHeader("X-User-Role") String role
    ) {
        return repository.findById(id)
            .map(order -> {
                if (!isAdmin(role) && !order.getUsername().equalsIgnoreCase(username)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<OrderResponse>build();
                }
                return ResponseEntity.ok(OrderResponse.from(order));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(username).stream().map(OrderResponse::from).toList());
    }

    @GetMapping("/backOffice/list")
    public ResponseEntity<List<OrderResponse>> getOrdersBackOffice(
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String username,
        @RequestHeader("X-User-Role") String role
    ) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String effectiveUsername = username != null && !username.isBlank() ? username : customerId;
        if (effectiveUsername == null || effectiveUsername.isBlank()) {
            return ResponseEntity.ok(repository.findAll().stream().map(OrderResponse::from).toList());
        }
        return ResponseEntity.ok(orderService.getOrdersByCustomer(effectiveUsername).stream().map(OrderResponse::from).toList());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
        @PathVariable Long id,
        @RequestParam OrderStatus status,
        @RequestHeader("X-User-Role") String role
    ) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @GetMapping("/backOffice/updateStatus")
    public ResponseEntity<Order> updateStatusBackOffice(
        @RequestParam Long orderId,
        @RequestParam Integer newStatus,
        @RequestHeader("X-User-Role") String role
    ) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        OrderStatus status = switch (newStatus) {
            case 1 -> OrderStatus.SHIPPED;
            case 2 -> OrderStatus.DELIVERED;
            case 3 -> OrderStatus.ONHOLD;
            case 4 -> OrderStatus.CANCELED;
            default -> OrderStatus.HANDLING;
        };
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SUPERADMIN".equalsIgnoreCase(role);
    }
}
