package com.xcommerce.order_service.repository;

import com.xcommerce.order_service.model.OrderOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, Long> {
}