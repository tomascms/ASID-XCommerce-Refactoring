package com.xcommerce.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Serviço de Autenticação (Porta 8081) - Online e a falar com a BD!";
    }
}