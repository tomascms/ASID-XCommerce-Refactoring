package com.xcommerce.cart_service.controller;

import com.xcommerce.cart_service.dto.CartItemRequest;
import com.xcommerce.cart_service.dto.CartProductRequest;
import com.xcommerce.cart_service.model.CartItem;
import com.xcommerce.cart_service.repository.CartRepository;
import jakarta.validation.Valid;
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

    @GetMapping("/get")
    public List<CartItem> getMyCartAlias(@RequestHeader("X-User-Name") String username) {
        return getMyCart(username);
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody CartItemRequest request, @RequestHeader("X-User-Name") String username) {
        CartItem item = new CartItem();
        item.setProductId(request.getProductId());
        item.setQuantity(request.getQuantity());
        item.setUsername(username);

        CartItem existing = repository.findByUsernameAndProductId(username, item.getProductId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
            return ResponseEntity.ok(repository.save(existing));
        }

        return ResponseEntity.ok(repository.save(item));
    }

    @PostMapping("/add")
    public ResponseEntity<CartItem> addToCartAlias(@Valid @RequestBody CartItemRequest item, @RequestHeader("X-User-Name") String username) {
        return addToCart(item, username);
    }

    @PatchMapping("/addProduct")
    public ResponseEntity<CartItem> addProductPatchAlias(@Valid @RequestBody CartItemRequest item, @RequestHeader("X-User-Name") String username) {
        return addToCart(item, username);
    }

    @PatchMapping("/decreaseProductQuantity")
    public ResponseEntity<List<CartItem>> decreaseProductQuantity(
        @Valid @RequestBody CartProductRequest request,
        @RequestHeader("X-User-Name") String username
    ) {
        CartItem item = repository.findByUsernameAndProductId(username, request.getProductId());
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        int delta = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        int newQuantity = item.getQuantity() - delta;
        if (newQuantity <= 0) {
            repository.delete(item);
        } else {
            item.setQuantity(newQuantity);
            repository.save(item);
        }

        return ResponseEntity.ok(repository.findByUsername(username));
    }

    @PatchMapping("/removeProduct")
    public ResponseEntity<List<CartItem>> removeProduct(
        @Valid @RequestBody CartProductRequest request,
        @RequestHeader("X-User-Name") String username
    ) {
        CartItem item = repository.findByUsernameAndProductId(username, request.getProductId());
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        repository.delete(item);
        return ResponseEntity.ok(repository.findByUsername(username));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearMyCart(@RequestHeader("X-User-Name") String username) {
        repository.deleteAll(repository.findByUsername(username));
        return ResponseEntity.noContent().build();
    }
}
