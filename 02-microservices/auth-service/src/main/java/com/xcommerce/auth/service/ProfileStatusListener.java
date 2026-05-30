package com.xcommerce.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcommerce.auth.dto.ProfileStatusEvent;
import com.xcommerce.auth.model.User;
import com.xcommerce.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ProfileStatusListener {
    private static final Logger log = LoggerFactory.getLogger(ProfileStatusListener.class);

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProfileStatusListener(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "profile-success-events", groupId = "auth-service-group")
    public void handleProfileSuccess(String message) {
        updateAccountStatus(message, "ACTIVE", true);
    }

    @KafkaListener(topics = "profile-failed-events", groupId = "auth-service-group")
    public void handleProfileFailed(String message) {
        try {
            ProfileStatusEvent event = objectMapper.readValue(message, ProfileStatusEvent.class);
            User user = userRepository.findByUsername(event.username());
            if (user != null) {
                userRepository.delete(user);
                log.warn("🧹 [AUTH] Credentials removed for {} after profile failure", event.username());
            }
        } catch (Exception exception) {
            log.error("❌ [AUTH] Failed to consume profile failure event: {}", exception.getMessage());
        }
    }

    private void updateAccountStatus(String message, String status, boolean active) {
        try {
            ProfileStatusEvent event = objectMapper.readValue(message, ProfileStatusEvent.class);
            User user = userRepository.findByUsername(event.username());
            if (user != null) {
                user.setStatus(status);
                user.setActive(active);
                userRepository.save(user);
                log.info("✅ [AUTH] Credentials activated for {}", event.username());
            }
        } catch (Exception exception) {
            log.error("❌ [AUTH] Failed to consume profile success event: {}", exception.getMessage());
        }
    }
}