package com.xcommerce.user_service.config;

import com.xcommerce.user_service.model.User;
import com.xcommerce.user_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            boolean changed = false;
            if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                changed = true;
            }
            if (user.getActive() == null) {
                user.setActive(Boolean.TRUE);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
        }
    }
}
