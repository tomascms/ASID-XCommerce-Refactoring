package com.xcommerce.cart_service.repository;

import com.xcommerce.cart_service.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUsername(String username);
    CartItem findByUsernameAndProductId(String username, Long productId);

    @Transactional
    void deleteByUsername(String username);
}
