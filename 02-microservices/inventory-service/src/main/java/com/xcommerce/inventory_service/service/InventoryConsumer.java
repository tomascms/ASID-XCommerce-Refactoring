package com.xcommerce.inventory_service.service;

import com.xcommerce.inventory_service.model.Inventory;
import com.xcommerce.inventory_service.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InventoryConsumer {
    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);

    @Autowired
    private InventoryRepository repository;

    @Transactional
    @KafkaListener(topics = "product-events", groupId = "inventory-group")
    public void handleProductCreated(String payload) {
        try {
            // Validate payload format
            if (payload == null || payload.isBlank()) {
                log.warn("⚠️ [INVENTORY] Payload inválido (vazio)");
                return;
            }
            
            String[] parts = payload.split(":");
            if (parts.length < 1) {
                log.warn("⚠️ [INVENTORY] Payload mal formatado: {}", payload);
                return;
            }
            
            Long productId = Long.parseLong(parts[0]);
            Integer quantity = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            log.info("📦 [INVENTORY] Novo produto detetado. Stock inicial para ID {}: {}", productId, quantity);

            Inventory stock = repository.findByProductId(productId).orElseGet(Inventory::new);
            stock.setProductId(productId);
            stock.setQuantity(quantity);
            repository.save(stock);
        } catch (NumberFormatException e) {
            log.error("❌ [INVENTORY] Erro ao parsear ID do produto ou quantidade: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ [INVENTORY] Erro ao processar evento de produto: {}", e.getMessage());
        }
    }

    @Transactional
    @KafkaListener(topics = "order-placed-events", groupId = "inventory-group")
    public void handleOrderPlaced(String message) {
        try {
            // Validate payload format
            if (message == null || message.isBlank()) {
                log.warn("⚠️ [INVENTORY] Message inválida (vazia)");
                return;
            }
            
            String[] parts = message.split(":");
            if (parts.length < 2) {
                log.warn("⚠️ [INVENTORY] Message mal formatada: {}", message);
                return;
            }
            
            Long productId = Long.parseLong(parts[0]);
            Integer quantityToReduce = Integer.parseInt(parts[1]);

            repository.findByProductId(productId).ifPresent(inventory -> {
                inventory.setQuantity(inventory.getQuantity() - quantityToReduce);
                repository.save(inventory);
                log.info("📦 [INVENTORY] Stock reduzido para o produto {}. Nova quantidade: {}", productId, inventory.getQuantity());
            });
        } catch (NumberFormatException e) {
            log.error("❌ [INVENTORY] Erro ao parsear dados da mensagem: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ [INVENTORY] Erro ao processar redução de stock: {}", e.getMessage());
        }
    }
}
