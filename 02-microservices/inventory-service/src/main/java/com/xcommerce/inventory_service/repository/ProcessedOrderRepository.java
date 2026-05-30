package com.xcommerce.inventory_service.repository;

import com.xcommerce.inventory_service.model.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {
}