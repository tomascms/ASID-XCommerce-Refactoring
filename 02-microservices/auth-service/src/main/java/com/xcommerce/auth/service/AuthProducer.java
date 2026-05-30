package com.xcommerce.auth.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthProducer {
    private static final Logger log = LoggerFactory.getLogger(AuthProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AuthProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendLoginEvent(String username) {
        String message = "User login detected: " + username;
        this.kafkaTemplate.send("user-events", username, message);
        log.info("🔐 [AUTH] Evento de login enviado para Kafka: {}", username);
    }
}