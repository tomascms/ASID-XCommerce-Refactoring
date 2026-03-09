package com.xcommerce.inventory_service.controller;

import com.xcommerce.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired private InventoryService service;

    @GetMapping("/check")
    public boolean checkStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        return service.isInStock(productId, quantity);
    }

    @PostMapping("/decrease")
    public void decrease(@RequestParam Long productId, @RequestParam Integer quantity) {
        service.decreaseStock(productId, quantity);
    }
}