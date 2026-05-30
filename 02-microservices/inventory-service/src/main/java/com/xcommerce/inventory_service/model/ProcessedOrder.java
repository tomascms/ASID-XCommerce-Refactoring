package com.xcommerce.inventory_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedOrder {
    @Id
    private Long orderId;
    private java.time.LocalDateTime processedAt;
}