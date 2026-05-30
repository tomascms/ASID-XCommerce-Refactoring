package com.xcommerce.identity.controller;

import com.xcommerce.identity.dto.UserRequest;
import com.xcommerce.identity.dto.UserResponse;
import com.xcommerce.identity.model.User;
import com.xcommerce.identity.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/rest/user")
public class UserController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    @Transactional
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        User savedUser = createOrUpdate(null, request, "CUSTOMER");
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    @PostMapping("/admin")
    @Transactional
    public ResponseEntity<UserResponse> createAdmin(
        @Valid @RequestBody UserRequest request,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        requireAdmin(requesterRole);
        User savedUser = createOrUpdate(null, request, "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(savedUser));
    }

    @PostMapping("/super-admin")
    @Transactional
    public ResponseEntity<UserResponse> createSuperAdmin(
        @Valid @RequestBody UserRequest request,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        requireSuperAdmin(requesterRole);
        User savedUser = createOrUpdate(null, request, "SUPERADMIN");
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
        @PathVariable @NonNull Long id,
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

    @PatchMapping("/{userId}")
    @Transactional
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable @NonNull Long userId,
        @RequestBody UserRequest data,
        @RequestHeader("X-User-Name") String requesterUsername,
        @RequestHeader("X-User-Role") String requesterRole
    ) {
        return repository.findById(userId).map(user -> {
            requireSameUserOrAdmin(user, requesterUsername, requesterRole);
            if (!isAdmin(requesterRole)) {
                data.setRole(null);
                data.setActive(null);
            }

            User saved = createOrUpdate(user, data, null);
            return ResponseEntity.ok(UserResponse.from(saved));
        }).orElse(ResponseEntity.notFound().build());
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

        return repository.save(user);
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
