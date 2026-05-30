package com.xcommerce.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcommerce.auth.dto.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistrationProducer {
    private static final Logger log = LoggerFactory.getLogger(RegistrationProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RegistrationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendUserCreatedEvent(String username, String email, String passwordHash, String role, String firstName, String lastName, String address) {
        try {
            String payload = objectMapper.writeValueAsString(new UserCreatedEvent(username, email, passwordHash, role, firstName, lastName, address));
            kafkaTemplate.send("user-created-events", username, payload);
            log.info("👤 [AUTH] User created event published: {}", username);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to publish user-created event", exception);
        }
    }
}