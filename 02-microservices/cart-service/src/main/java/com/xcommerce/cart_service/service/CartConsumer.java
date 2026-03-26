package com.xcommerce.cart_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.xcommerce.cart_service.repository.CartRepository; 
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CartConsumer {
    private static final Logger log = LoggerFactory.getLogger(CartConsumer.class);

    @Autowired
    private CartRepository cartRepository;

    @Transactional
    @KafkaListener(topics = "user-events", groupId = "cart-group")
    public void handleUserLogin(String username) {
        log.info("🛒 [CART] Login detetado para o utilizador: {}. A verificar carrinho...", username);
        try {
            cartRepository.deleteByUsername(username); 
            log.info("✅ Sucesso: O carrinho de '{}' foi limpo.", username);
        } catch (Exception e) {
            log.error("❌ Erro ao limpar carrinho via Kafka: {}", e.getMessage());
        }
    }
}