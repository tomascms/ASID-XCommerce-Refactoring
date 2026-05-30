package com.xcommerce.identity.config;

import com.xcommerce.identity.model.User;
import com.xcommerce.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("🔍 Verificando base de dados de identidade...");

        User admin = userRepository.findByUsernameOrEmail("admin", "admin@xcommerce.com");
        if (admin == null) {
            admin = new User();
            log.info("➕ Admin nao encontrado. Criando utilizador admin padrao.");
        } else {
            log.info("♻️ Admin encontrado. Garantindo credenciais e role corretas.");
        }

        admin.setUsername("admin");
        admin.setEmail("admin@xcommerce.com");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setFirstName("Admin");
        admin.setLastName("XCommerce");
        admin.setAddress("HQ");
        admin.setRole("ADMIN");
        admin.setActive(true);

        userRepository.save(admin);
        log.info("✅ Utilizador Admin garantido com role ADMIN.");
    }
}
