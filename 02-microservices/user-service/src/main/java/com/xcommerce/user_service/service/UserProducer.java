package com.xcommerce.user_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserCreatedEvent(String username) {
        this.kafkaTemplate.send("user-events", username, "USER_CREATED");
        System.out.println("👤 Kafka [User]: Evento de novo utilizador enviado: " + username);
    }
}