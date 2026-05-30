package com.xcommerce.order_service.config;

import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.model.OrderStatus;
import com.xcommerce.order_service.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderDataCleanupRunner implements CommandLineRunner {

    private final OrderRepository orderRepository;

    public OrderDataCleanupRunner(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        for (Order order : orderRepository.findAll()) {
            if (order.getUsername() == null || order.getUsername().isBlank()
                || order.getProductId() == null
                || order.getQuantity() == null
                || order.getQuantity() < 1) {
                orderRepository.delete(order);
                continue;
            }

            try {
                boolean changed = false;
                if (order.getStatus() == null) {
                    order.setStatus(OrderStatus.HANDLING);
                    changed = true;
                }
                if (order.getOrderDate() == null) {
                    order.setOrderDate(LocalDateTime.now());
                    changed = true;
                }
                if (changed) {
                    orderRepository.save(order);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
