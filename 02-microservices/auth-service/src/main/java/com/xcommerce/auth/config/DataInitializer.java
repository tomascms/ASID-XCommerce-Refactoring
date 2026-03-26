package com.xcommerce.auth.config;

import com.xcommerce.auth.dto.UserSyncRequest;
import com.xcommerce.auth.model.User;
import com.xcommerce.auth.repository.UserRepository;
import com.xcommerce.auth.service.UserSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSyncService userSyncService;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, UserSyncService userSyncService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSyncService = userSyncService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🔍 Verificando base de dados de autenticação...");
        
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin@xcommerce.com");
            admin.setEmail("admin@xcommerce.com");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRole("ADMIN");
            admin.setActive(true);
            
            userRepository.save(admin);
            try {
                userSyncService.sync(new UserSyncRequest(
                    admin.getUsername(),
                    admin.getEmail(),
                    admin.getPassword(),
                    admin.getRole(),
                    admin.getActive(),
                    "Admin",
                    "XCommerce",
                    "HQ"
                ));
            } catch (Exception ignored) {
                log.warn("⚠️ Warning ao sincronizar admin user");
            }
            log.info("✅ Utilizador Admin criado com sucesso!");
        } else {
            log.info("✓ Base de dados já contém utilizadores.");
        }
    }
} 
