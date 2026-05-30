package com.xcommerce.inventory_service.controller;

import com.xcommerce.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService service;

    @GetMapping("/check")
    public boolean checkStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        return service.isInStock(productId, quantity);
    }

    @PostMapping("/decrease")
    public void decrease(@RequestParam Long productId, @RequestParam Integer quantity) {
        service.decreaseStock(productId, quantity);
    }

    @PostMapping("/sync")
    public void sync(@RequestParam Long productId, @RequestParam Integer quantity) {
        service.syncStock(productId, quantity);
    }
}
