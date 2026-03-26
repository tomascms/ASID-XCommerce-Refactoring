package com.xcommerce.user_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@SuppressWarnings("unchecked")
public class UserProducer {
    private static final Logger log = LoggerFactory.getLogger(UserProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserCreatedEvent(String username) {
        this.kafkaTemplate.send("user-events", username, "USER_CREATED");
        log.info("👤 [USER] Evento de novo utilizador enviado: {}", username);
    }
}