package com.xcommerce.user_service.config;

import com.xcommerce.user_service.dto.AuthUserSyncRequest;
import com.xcommerce.user_service.model.User;
import com.xcommerce.user_service.repository.UserRepository;
import com.xcommerce.user_service.service.AuthSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AuthSyncBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthSyncService authSyncService;

    public AuthSyncBootstrapRunner(UserRepository userRepository, AuthSyncService authSyncService) {
        this.userRepository = userRepository;
        this.authSyncService = authSyncService;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
                continue;
            }

            try {
                authSyncService.sync(new AuthUserSyncRequest(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole(),
                    user.getActive(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getAddress()
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
