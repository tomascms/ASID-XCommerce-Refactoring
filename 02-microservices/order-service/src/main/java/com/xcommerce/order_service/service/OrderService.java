package com.xcommerce.order_service.service;

import com.xcommerce.order_service.dto.CartItemDTO;
import com.xcommerce.order_service.dto.CreateOrderRequest;
import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.model.OrderItem;
import com.xcommerce.order_service.model.OrderStatus;
import com.xcommerce.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    @Autowired private OrderRepository repository;
    @Autowired private OrderRemoteService orderRemoteService;
    @Autowired private OrderProducer orderProducer;

    @Transactional
    public Order createOrder(String username) {
        List<CartItemDTO> cartItems = orderRemoteService.getCartItems(username);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Carrinho vazio!");
        }

        List<CreateOrderRequest> requests = cartItems.stream().map(item -> {
            CreateOrderRequest request = new CreateOrderRequest();
            request.setUsername(username);
            request.setProductId(item.getProductId());
            request.setQuantity(item.getQuantity());
            return request;
        }).toList();

        Order savedOrder = buildAndPersistOrder(username, requests);
        orderRemoteService.clearCart(username);
        return savedOrder;
    }

    @Transactional
    public Order createDirectOrder(String fallbackUsername, CreateOrderRequest request) {
        String username = request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : fallbackUsername;
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username obrigatorio.");
        }
        return buildAndPersistOrder(username, List.of(request));
    }

    public List<Order> getOrdersByCustomer(String username) {
        return repository.findByUsernameOrderByOrderDateDesc(username);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(newStatus);
        return repository.save(order);
    }

    private Order buildAndPersistOrder(String username, List<CreateOrderRequest> requests) {
        for (CreateOrderRequest request : requests) {
            boolean hasStock = orderRemoteService.checkStock(request.getProductId(), request.getQuantity());
            if (!hasStock) {
                throw new RuntimeException("Stock insuficiente para o produto ID: " + request.getProductId());
            }
        }

        Order order = new Order();
        order.setUsername(username);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.HANDLING);

        List<OrderItem> items = requests.stream().map(request -> {
            OrderItem item = new OrderItem();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            BigDecimal productPrice = orderRemoteService.getProductPrice(request.getProductId());
            item.setPrice(productPrice.doubleValue());
            return item;
        }).toList();

        order.setItems(items);
        order.setProductId(requests.getFirst().getProductId());
        order.setQuantity(requests.stream().mapToInt(CreateOrderRequest::getQuantity).sum());
        order.setTotalAmount(items.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum());

        for (CreateOrderRequest request : requests) {
            orderRemoteService.decreaseStock(request.getProductId(), request.getQuantity());
        }

        Order savedOrder = repository.save(order);
        orderProducer.sendOrderEvents(savedOrder.getUsername(), savedOrder.getProductId(), savedOrder.getQuantity());
        return savedOrder;
    }
}
