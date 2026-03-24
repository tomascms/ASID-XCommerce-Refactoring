package com.xcommerce.auth.config;

import com.xcommerce.auth.dto.UserSyncRequest;
import com.xcommerce.auth.model.User;
import com.xcommerce.auth.repository.UserRepository;
import com.xcommerce.auth.service.UserSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class UserSyncBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserSyncService userSyncService;

    public UserSyncBootstrapRunner(UserRepository userRepository, UserSyncService userSyncService) {
        this.userRepository = userRepository;
        this.userSyncService = userSyncService;
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
                userSyncService.sync(new UserSyncRequest(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole(),
                    user.getActive(),
                    null,
                    null,
                    null
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
