package com.xcommerce.auth.controller;

import com.xcommerce.auth.dto.RegisterRequest;
import com.xcommerce.auth.dto.UserSyncRequest;
import com.xcommerce.auth.model.User;
import com.xcommerce.auth.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xcommerce.auth.dto.LoginRequest;
import com.xcommerce.auth.dto.LoginResponse;
import com.xcommerce.auth.service.AuthProducer;
import com.xcommerce.auth.service.RegistrationProducer;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/rest/user")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthProducer authProducer; 

    @Autowired
    private RegistrationProducer registrationProducer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest data) {
        String username = data.username() != null && !data.username().isBlank() ? data.username() : data.email();
        String email = data.email() != null && !data.email().isBlank() ? data.email() : username;

        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Username, email e password sao obrigatorios.");
        }

        if (userRepository.findByUsernameOrEmail(username, email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Usuario ja existe");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(data.password()));
        user.setRole("CUSTOMER");
        user.setStatus("PENDING");
        user.setActive(false);
        userRepository.save(user);
        registrationProducer.sendUserCreatedEvent(
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            data.firstName(),
            data.lastName(),
            data.address()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new LoginResponse("Registration accepted; awaiting profile provisioning."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest data) {
        User user = userRepository.findByUsernameOrEmail(data.username(), data.username());

        if (user != null) {
            authProducer.sendLoginEvent(user.getUsername());
            return ResponseEntity.status(HttpStatus.GONE).body(new LoginResponse("Use Keycloak to authenticate; auth-service no longer issues tokens."));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid credentials."));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest data) {
        return login(data);
    }

    @PostMapping("/internal/sync")
    @Transactional
    public ResponseEntity<Void> syncInternal(@RequestBody UserSyncRequest request) {
        if (request.username() == null || request.username().isBlank() || request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findByUsernameOrEmail(request.username(), request.email());
        if (user == null) {
            user = new User();
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.passwordHash() != null && !request.passwordHash().isBlank()) {
            user.setPassword(request.passwordHash());
        }
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(request.role());
        }
        if (request.status() != null && !request.status().isBlank()) {
            user.setStatus(request.status());
        }
        user.setActive(request.active() == null ? Boolean.TRUE : request.active());
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
