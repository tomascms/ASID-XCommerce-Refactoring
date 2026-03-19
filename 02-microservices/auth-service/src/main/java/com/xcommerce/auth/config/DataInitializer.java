package com.xcommerce.auth.config;

import com.xcommerce.auth.model.User;
import com.xcommerce.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> Verificando base de dados...");
        
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@xcommerce.com");
            admin.setPassword("123456");
            admin.setRole("ADMIN");
            
            userRepository.save(admin);
            System.out.println(">>> Utilizador Admin criado com sucesso!");
        } else {
            System.out.println(">>> Base de dados já contém utilizadores.");
        }
    }
} 