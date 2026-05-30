package com.xcommerce.inventory_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "order-cancelled-events", groupId = "inventory-group")
    public void handleOrderCancelled(Map<String, Object> event) {
        try {
            Object orderId = event.get("orderId");
            log.info("🔄 [INVENTORY] Recebido evento de compensação para Order: {}", orderId);
            
            if (event.get("items") == null) {
                log.warn("⚠️ [INVENTORY] Evento de cancelamento sem itens para compensar.");
                return;
            }
            String itemsPayload = event.get("items").toString();

            for (String itemStr : itemsPayload.split(",")) {
                String[] parts = itemStr.split(":");
                Long productId = Long.parseLong(parts[0]);
                Integer quantity = Integer.parseInt(parts[1]);
                
                inventoryService.compensateStock(productId, quantity);
                log.info("✅ [INVENTORY] Stock reposto: Produto {} Qtd {}", productId, quantity);
            }
        } catch (Exception e) {
            log.error("❌ [INVENTORY] Erro ao processar compensação de stock: {}", e.getMessage());
        }
    }
}