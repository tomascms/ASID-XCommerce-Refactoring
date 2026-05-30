package com.xcommerce.identity.repository;

import com.xcommerce.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    User findByEmail(String email);
    User findByUsername(String username);
    User findByUsernameOrEmail(String username, String email);
}
