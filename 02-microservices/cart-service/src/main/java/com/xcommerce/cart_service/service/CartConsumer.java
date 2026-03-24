package com.xcommerce.cart_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.xcommerce.cart_service.repository.CartRepository; 
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartConsumer {

    @Autowired
    private CartRepository cartRepository;

    @Transactional
    @KafkaListener(topics = "user-events", groupId = "cart-group")
    public void handleUserLogin(String username) {
        System.out.println("🛒 Kafka: Login detetado para o utilizador: " + username + ". A verificar carrinho...");
        try {
            cartRepository.deleteByUsername(username); 
            System.out.println("✅ Sucesso: O carrinho de '" + username + "' foi limpo.");
        } catch (Exception e) {
            System.err.println("❌ Erro ao limpar carrinho via Kafka: " + e.getMessage());
        }
    }
}