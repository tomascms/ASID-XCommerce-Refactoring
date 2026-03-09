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
        Inventory inv = repository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        
        if (inv.getQuantity() < quantity) {
            throw new RuntimeException("Stock insuficiente");
        }
        
        inv.setQuantity(inv.getQuantity() - quantity);
        repository.save(inv);
    }
}