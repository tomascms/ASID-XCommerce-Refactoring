package com.xcommerce.order_service.service;

import com.xcommerce.order_service.client.CartClient;
import com.xcommerce.order_service.dto.CartItemDTO;
import com.xcommerce.order_service.model.*;
import com.xcommerce.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    @Autowired private OrderRepository repository;
    @Autowired private CartClient cartClient;

    public Order createOrder(String username) {
        List<CartItemDTO> cartItems = cartClient.getCartItems(username);
        if(cartItems.isEmpty()) throw new RuntimeException("Carrinho vazio!");

        Order order = new Order();
        order.setUsername(username);
        order.setOrderDate(LocalDateTime.now());
        
        List<OrderItem> items = cartItems.stream().map(c -> {
            OrderItem i = new OrderItem();
            i.setProductId(c.getProductId());
            i.setQuantity(c.getQuantity());
            i.setPrice(10.0); // Preço simulado
            return i;
        }).toList();

        order.setItems(items);
        order.setTotalAmount(items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());

        Order savedOrder = repository.save(order);
        cartClient.clearCart(username); 
        return savedOrder;
    }
}