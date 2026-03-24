package com.xcommerce.inventory_service.service;

import com.xcommerce.inventory_service.model.Inventory;
import com.xcommerce.inventory_service.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryConsumer {

    @Autowired
    private InventoryRepository repository;

    @Transactional
    @KafkaListener(topics = "product-events", groupId = "inventory-group")
    public void handleProductCreated(String payload) {
        String[] parts = payload.split(":");
        Long productId = Long.parseLong(parts[0]);
        Integer quantity = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        System.out.println("Kafka [Inventory]: Novo produto detetado. Stock inicial para ID " + productId + ": " + quantity);

        Inventory stock = repository.findByProductId(productId).orElseGet(Inventory::new);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        repository.save(stock);
    }

    @Transactional
    @KafkaListener(topics = "order-placed-events", groupId = "inventory-group")
    public void handleOrderPlaced(String message) {
        try {
            String[] parts = message.split(":");
            Long productId = Long.parseLong(parts[0]);
            Integer quantityToReduce = Integer.parseInt(parts[1]);

            repository.findByProductId(productId).ifPresent(inventory -> {
                inventory.setQuantity(inventory.getQuantity() - quantityToReduce);
                repository.save(inventory);
                System.out.println("Kafka [Inventory]: Stock reduzido para o produto " + productId + ". Nova quantidade: " + inventory.getQuantity());
            });
        } catch (Exception e) {
            System.err.println("Erro ao processar reducao de stock: " + e.getMessage());
        }
    }
}
