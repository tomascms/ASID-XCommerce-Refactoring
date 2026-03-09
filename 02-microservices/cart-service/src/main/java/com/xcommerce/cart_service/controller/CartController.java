package com.xcommerce.cart_service.controller;

import com.xcommerce.cart_service.model.CartItem;
import com.xcommerce.cart_service.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartRepository repository;

    @GetMapping
    public List<CartItem> getMyCart(@RequestHeader("X-User-Name") String username) {
        return repository.findByUsername(username);
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem item, @RequestHeader("X-User-Name") String username) {
        System.out.println("A adicionar ao carrinho para o user: " + username);
        item.setUsername(username);
        return ResponseEntity.ok(repository.save(item));
    }
}
