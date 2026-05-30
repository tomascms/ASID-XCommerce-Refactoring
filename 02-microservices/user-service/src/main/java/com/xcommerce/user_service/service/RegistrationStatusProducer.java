package com.xcommerce.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcommerce.user_service.dto.ProfileStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistrationStatusProducer {
    private static final Logger log = LoggerFactory.getLogger(RegistrationStatusProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RegistrationStatusProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendProfileSuccess(String username) {
        publish("profile-success-events", new ProfileStatusEvent(username, "ACTIVE", null));
    }

    public void sendProfileFailed(String username, String reason) {
        publish("profile-failed-events", new ProfileStatusEvent(username, "FAILED", reason));
    }

    private void publish(String topic, ProfileStatusEvent event) {
        try {
            kafkaTemplate.send(topic, event.username(), objectMapper.writeValueAsString(event));
            log.info("📨 [USER] Published {} for {}", topic, event.username());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to publish registration status event", exception);
        }
    }
}