package com.xcommerce.cart_service.config;

import com.xcommerce.cart_service.model.CartItem;
import com.xcommerce.cart_service.repository.CartRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CartDataCleanupRunner implements CommandLineRunner {

    private final CartRepository cartRepository;

    public CartDataCleanupRunner(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public void run(String... args) {
        for (CartItem item : cartRepository.findAll()) {
            if (item.getUsername() == null || item.getUsername().isBlank()
                || item.getProductId() == null
                || item.getQuantity() == null
                || item.getQuantity() < 1) {
                cartRepository.delete(item);
            }
        }
    }
}
