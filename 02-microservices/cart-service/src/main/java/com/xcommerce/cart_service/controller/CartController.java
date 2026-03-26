package com.xcommerce.cart_service.controller;

import com.xcommerce.cart_service.dto.CartItemRequest;
import com.xcommerce.cart_service.dto.CartProductRequest;
import com.xcommerce.cart_service.model.CartItem;
import com.xcommerce.cart_service.dto.CartItemResponse;
import com.xcommerce.cart_service.repository.CartRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@SuppressWarnings("unchecked")
public class CartController {

    @Autowired
    private CartRepository repository;

    @GetMapping
    public List<CartItemResponse> getMyCart(@RequestHeader("X-User-Name") String username) {
        return repository.findByUsername(username).stream().map(CartItemResponse::from).toList();
    }

    @GetMapping("/get")
    public List<CartItemResponse> getMyCartAlias(@RequestHeader("X-User-Name") String username) {
        return getMyCart(username);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CartItemResponse> addToCart(@Valid @RequestBody CartItemRequest request, @RequestHeader("X-User-Name") String username) {
        CartItem item = new CartItem();
        item.setProductId(request.getProductId());
        item.setQuantity(request.getQuantity());
        item.setUsername(username);

        CartItem existing = repository.findByUsernameAndProductId(username, item.getProductId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
            return ResponseEntity.ok(CartItemResponse.from(repository.save(existing)));
        }

        return ResponseEntity.ok(CartItemResponse.from(repository.save(item)));
    }

    @PostMapping("/add")
    @Transactional
    public ResponseEntity<CartItemResponse> addToCartAlias(@Valid @RequestBody CartItemRequest item, @RequestHeader("X-User-Name") String username) {
        return addToCart(item, username);
    }

    @PatchMapping("/addProduct")
    @Transactional
    public ResponseEntity<CartItemResponse> addProductPatchAlias(@Valid @RequestBody CartItemRequest item, @RequestHeader("X-User-Name") String username) {
        return addToCart(item, username);
    }

    @PatchMapping("/decreaseProductQuantity")
    @Transactional
    public ResponseEntity<List<CartItemResponse>> decreaseProductQuantity(
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

        return ResponseEntity.ok(repository.findByUsername(username).stream().map(CartItemResponse::from).toList());
    }

    @PatchMapping("/removeProduct")
    @Transactional
    public ResponseEntity<List<CartItemResponse>> removeProduct(
        @Valid @RequestBody CartProductRequest request,
        @RequestHeader("X-User-Name") String username
    ) {
        CartItem item = repository.findByUsernameAndProductId(username, request.getProductId());
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        repository.delete(item);
        return ResponseEntity.ok(repository.findByUsername(username).stream().map(CartItemResponse::from).toList());
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> clearMyCart(@RequestHeader("X-User-Name") String username) {
        repository.deleteAll(repository.findByUsername(username));
        return ResponseEntity.noContent().build();
    }
}
