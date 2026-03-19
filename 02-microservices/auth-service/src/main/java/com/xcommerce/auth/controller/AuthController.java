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
import com.xcommerce.auth.service.AuthProducer;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthProducer authProducer; 

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest data) {
        if("admin".equals(data.username()) && "1234".equals(data.password())) {
            String token = tokenService.generateToken(data.username());
            
            authProducer.sendLoginEvent(data.username());
            
            return ResponseEntity.ok(new LoginResponse(token));
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}