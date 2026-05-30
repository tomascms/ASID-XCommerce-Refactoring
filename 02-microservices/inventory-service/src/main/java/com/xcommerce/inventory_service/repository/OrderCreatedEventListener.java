package com.xcommerce.inventory_service.repository;

import com.xcommerce.inventory_service.service.InventoryService;
import com.xcommerce.inventory_service.model.ProcessedOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderCreatedEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ProcessedOrderRepository processedOrderRepository;

    public OrderCreatedEventListener(InventoryService inventoryService, KafkaTemplate<String, String> kafkaTemplate, ProcessedOrderRepository processedOrderRepository) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
        this.processedOrderRepository = processedOrderRepository;
    }

    @Transactional
    @KafkaListener(topics = "order-created-events", groupId = "inventory-group")
    public void handleOrderCreatedEvent(
            String message,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        
        Long orderId = Long.parseLong(key);

        // Idempotência: Se já existe na tabela de ordens processadas, ignoramos
        if (processedOrderRepository.existsById(orderId)) {
            log.info("⚠️ [INVENTORY] Ordem {} já processada anteriormente. Ignorando duplicado.", orderId);
            return;
        }

        log.info("📦 [INVENTORY] Processando reserva de stock para Order: {}", orderId);

        String itemsPart = message.split("items:")[1];
        String[] items = itemsPart.split(",");

        List<ReservedItem> successfullyReserved = new ArrayList<>();
        boolean allReserved = true;
        String firstFailureReason = "";

        try {
            for (String itemStr : items) {
                String[] parts = itemStr.split(":");
                Long productId = Long.parseLong(parts[0]);
                Integer qty = Integer.parseInt(parts[1]);

                try {
                    inventoryService.decreaseStock(productId, qty);
                    successfullyReserved.add(new ReservedItem(productId, qty));
                } catch (Exception e) {
                    allReserved = false;
                    firstFailureReason = e.getMessage();
                    break;
                }
            }

            if (allReserved) {
                log.info("✅ [INVENTORY] Stock reservado com sucesso para Order: {}", orderId);
                processedOrderRepository.save(new ProcessedOrder(orderId, LocalDateTime.now()));
                kafkaTemplate.send("stock-reservation-events", key, "status:STOCK_RESERVED_SUCCESS");
            } else {
                log.warn("❌ [INVENTORY] Falha na reserva. Rollback para Order: {}", orderId);
                for (ReservedItem reserved : successfullyReserved) {
                    inventoryService.compensateStock(reserved.productId, reserved.quantity);
                }
                kafkaTemplate.send("stock-reservation-events", key, "status:STOCK_RESERVATION_FAILED,reason:" + firstFailureReason);
            }

        } catch (Exception e) {
            log.error("❌ [INVENTORY] Erro crítico na Saga de Inventário: {}", e.getMessage());
        }
    }

    private static class ReservedItem {
        final Long productId;
        final Integer quantity;

        ReservedItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}