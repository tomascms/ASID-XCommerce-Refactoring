package com.xcommerce.identity.controller;

import com.xcommerce.identity.dto.LoginRequest;
import com.xcommerce.identity.dto.LoginResponse;
import com.xcommerce.identity.dto.RegisterRequest;
import com.xcommerce.identity.model.User;
import com.xcommerce.identity.repository.UserRepository;
import com.xcommerce.identity.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/user")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest data) {
        String username = data.username() != null && !data.username().isBlank() ? data.username() : data.email();
        String email = data.email() != null && !data.email().isBlank() ? data.email() : username;

        if (username == null || username.isBlank() || email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(new LoginResponse("Username, email e password sao obrigatorios."));
        }

        if (userRepository.findByUsernameOrEmail(username, email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new LoginResponse("Usuario ja existe."));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(data.password()));
        user.setFirstName(data.firstName());
        user.setLastName(data.lastName());
        user.setAddress(data.address());
        user.setRole("CUSTOMER");
        user.setActive(true);
        userRepository.save(user);

        String token = tokenService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest data) {
        User user = userRepository.findByUsernameOrEmail(data.username(), data.username());

        if (user != null
                && Boolean.TRUE.equals(user.getActive())
                && passwordEncoder.matches(data.password(), user.getPassword())) {
            String subject = user.getUsername() != null && !user.getUsername().isBlank() ? user.getUsername() : user.getEmail();
            String token = tokenService.generateToken(subject, user.getRole());
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid credentials."));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest data) {
        return login(data);
    }
}
