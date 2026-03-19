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

    // 1. Ouvir o Catalog para criar stock inicial
    @Transactional
    @KafkaListener(topics = "product-events", groupId = "inventory-group")
    public void handleProductCreated(String productId) {
        System.out.println("📦 Kafka [Inventory]: Novo produto detetado. Criando registo de stock para ID: " + productId);
        
        Long pId = Long.parseLong(productId);
        
        if (!repository.existsByProductId(pId)) {
            Inventory stock = new Inventory();
            stock.setProductId(pId);
            stock.setQuantity(0);
            repository.save(stock);
            System.out.println("✅ Stock inicializado para o produto " + pId);
        }
    }

    // 2. Ouvir o Order para abater stock
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
                System.out.println("📉 Stock reduzido para o produto " + productId + ". Nova quantidade: " + inventory.getQuantity());
            });
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar redução de stock: " + e.getMessage());
        }
    }
}