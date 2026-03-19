package com.xcommerce.user_service.controller;

import com.xcommerce.user_service.model.User;
import com.xcommerce.user_service.repository.UserRepository;
import com.xcommerce.user_service.service.UserProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserProducer userProducer;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = repository.save(user);
        
        userProducer.sendUserCreatedEvent(savedUser.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @GetMapping
    public List<User> getAll() {
        return repository.findAll();
    }
}