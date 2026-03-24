package com.xcommerce.user_service.controller;

import com.xcommerce.user_service.dto.AuthUserSyncRequest;
import com.xcommerce.user_service.dto.UserRequest;
import com.xcommerce.user_service.dto.UserResponse;
import com.xcommerce.user_service.model.User;
import com.xcommerce.user_service.repository.UserRepository;
import com.xcommerce.user_service.service.AuthSyncService;
import com.xcommerce.user_service.service.UserProducer;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserProducer userProducer;

    @Autowired
    private AuthSyncService authSyncService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        User savedUser = createOrUpdate(null, request, "CUSTOMER");
        userProducer.sendUserCreatedEvent(savedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    @PostMapping("/admin")
    public ResponseEntity<UserResponse> createAdmin(
        @Valid @RequestBody UserRequest request,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        requireAdmin(requesterRole);
        User savedUser = createOrUpdate(null, request, "ADMIN");
        userProducer.sendUserCreatedEvent(savedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    @PostMapping("/super-admin")
    public ResponseEntity<UserResponse> createSuperAdmin(
        @Valid @RequestBody UserRequest request,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        requireSuperAdmin(requesterRole);
        User savedUser = createOrUpdate(null, request, "SUPERADMIN");
        userProducer.sendUserCreatedEvent(savedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    @GetMapping
    public List<UserResponse> getAll(@RequestHeader("X-User-Role") String requesterRole) {
        requireAdmin(requesterRole);
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/backOffice/list")
    public List<UserResponse> getAllBackOffice(@RequestHeader("X-User-Role") String requesterRole) {
        requireAdmin(requesterRole);
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
        @PathVariable Long id,
        @RequestHeader("X-User-Name") String requesterUsername,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        return repository.findById(id)
            .map(user -> {
                requireSameUserOrAdmin(user, requesterUsername, requesterRole);
                return ResponseEntity.ok(UserResponse.from(user));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponse> getByUsername(
        @PathVariable String username,
        @RequestHeader("X-User-Name") String requesterUsername,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        User user = repository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        requireSameUserOrAdmin(user, requesterUsername, requesterRole);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @RequestBody UserRequest data,
        @RequestHeader("X-User-Name") String requesterUsername,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        return repository.findById(id).map(user -> {
            requireSameUserOrAdmin(user, requesterUsername, requesterRole);
            if (!isAdmin(requesterRole)) {
                data.setRole(null);
                data.setActive(null);
            }

            User saved = createOrUpdate(user, data, null);
            return ResponseEntity.ok(UserResponse.from(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/internal/sync")
    public ResponseEntity<Void> syncInternal(@RequestBody AuthUserSyncRequest request) {
        if (request.username() == null || request.username().isBlank() || request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User user = repository.findByUsernameOrEmail(request.username(), request.email());
        if (user == null) {
            user = new User();
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.passwordHash() != null && !request.passwordHash().isBlank()) {
            user.setPassword(request.passwordHash());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        repository.save(user);
        return ResponseEntity.ok().build();
    }

    private User createOrUpdate(User existingUser, UserRequest request, String forcedRole) {
        User user = existingUser != null ? existingUser : new User();
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        } else if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username obrigatorio.");
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        } else if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email obrigatorio.");
        }

        String rawPassword = request.getNewPassword() != null && !request.getNewPassword().isBlank()
            ? request.getNewPassword()
            : request.getPassword();
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        } else if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password obrigatoria.");
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (forcedRole != null) {
            user.setRole(forcedRole);
        } else if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        } else if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("CUSTOMER");
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        } else if (user.getActive() == null) {
            user.setActive(Boolean.TRUE);
        }

        User savedUser = repository.save(user);
        authSyncService.sync(new AuthUserSyncRequest(
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getPassword(),
            savedUser.getRole(),
            savedUser.getActive(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getAddress()
        ));
        return savedUser;
    }

    private void requireSameUserOrAdmin(User targetUser, String requesterUsername, String requesterRole) {
        if (!isAdmin(requesterRole) && !targetUser.getUsername().equalsIgnoreCase(requesterUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
    }

    private void requireAdmin(String requesterRole) {
        if (!isAdmin(requesterRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
    }

    private void requireSuperAdmin(String requesterRole) {
        if (!"SUPERADMIN".equalsIgnoreCase(requesterRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
    }

    private boolean isAdmin(String requesterRole) {
        return "ADMIN".equalsIgnoreCase(requesterRole) || "SUPERADMIN".equalsIgnoreCase(requesterRole);
    }
}
