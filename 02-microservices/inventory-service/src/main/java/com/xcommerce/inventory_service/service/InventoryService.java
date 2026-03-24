package com.xcommerce.inventory_service.service;

import com.xcommerce.inventory_service.model.Inventory;
import com.xcommerce.inventory_service.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository repository;

    @Transactional(readOnly = true)
    public boolean isInStock(Long productId, Integer quantity) {
        return repository.findByProductId(productId)
            .map(inv -> inv.getQuantity() >= quantity)
            .orElse(false);
    }

    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Inventory inventory = repository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Produto nao encontrado"));

        if (inventory.getQuantity() < quantity) {
            throw new RuntimeException("Stock insuficiente");
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        repository.save(inventory);
    }

    @Transactional
    public Inventory syncStock(Long productId, Integer quantity) {
        Inventory inventory = repository.findByProductId(productId).orElseGet(Inventory::new);
        inventory.setProductId(productId);
        inventory.setQuantity(quantity);
        return repository.save(inventory);
    }
}
