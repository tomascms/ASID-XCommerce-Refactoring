package com.xcommerce.user_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcommerce.user_service.dto.UserCreatedEvent;
import com.xcommerce.user_service.model.User;
import com.xcommerce.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserCreatedEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventListener.class);

    private final UserRepository userRepository;
    private final RegistrationStatusProducer registrationStatusProducer;
    private final ObjectMapper objectMapper;

    public UserCreatedEventListener(UserRepository userRepository, RegistrationStatusProducer registrationStatusProducer, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.registrationStatusProducer = registrationStatusProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-created-events", groupId = "user-service-group")
    public void handleUserCreated(String message) {
        try {
            UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
            User user = userRepository.findByUsernameOrEmail(event.username(), event.email());
            if (user == null) {
                user = new User();
            }

            user.setUsername(event.username());
            user.setEmail(event.email());
            user.setPassword(event.passwordHash());
            user.setFirstName(event.firstName());
            user.setLastName(event.lastName());
            user.setAddress(event.address());
            user.setRole(event.role() == null || event.role().isBlank() ? "CUSTOMER" : event.role());
            user.setAccountStatus("PENDING");
            user.setActive(Boolean.FALSE);
            userRepository.save(user);

            registrationStatusProducer.sendProfileSuccess(event.username());
            log.info("👤 [USER] Profile provisioned for {}", event.username());
        } catch (Exception exception) {
            log.error("❌ [USER] Failed to provision profile: {}", exception.getMessage());
            try {
                UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
                registrationStatusProducer.sendProfileFailed(event.username(), exception.getMessage());
            } catch (Exception secondaryException) {
                log.error("❌ [USER] Failed to publish profile failure event: {}", secondaryException.getMessage());
            }
        }
    }
}