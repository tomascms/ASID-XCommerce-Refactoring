package com.xcommerce.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xcommerce.auth.dto.LoginRequest;
import com.xcommerce.auth.dto.LoginResponse;
import com.xcommerce.auth.service.TokenService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest data) {
        // Simulação de login para admin
        if("admin".equals(data.username()) && "1234".equals(data.password())) {
            String token = tokenService.generateToken(data.username());
            // Agora o ResponseEntity sabe que devolve um LoginResponse
            return ResponseEntity.ok(new LoginResponse(token));
        }
        // No erro, usamos o operador diamante <> para manter a tipagem
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}