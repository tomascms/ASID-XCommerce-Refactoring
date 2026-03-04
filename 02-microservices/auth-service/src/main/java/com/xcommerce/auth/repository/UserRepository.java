package com.xcommerce.auth.repository;

import com.xcommerce.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Isto permite procurar um utilizador pelo email (muito útil para o Login)
    User findByEmail(String email);
}