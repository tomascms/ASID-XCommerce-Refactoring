package com.xcommerce.auth.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AuthProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendLoginEvent(String username) {
        String message = "User login detected: " + username;
        this.kafkaTemplate.send("user-events", username, message);
        System.out.println("🚀 Evento de login enviado para o Kafka: " + username);
    }
}