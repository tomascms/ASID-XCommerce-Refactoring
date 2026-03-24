package com.xcommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    String username,
    @NotBlank String password,
    @Email String email,
    String firstName,
    String lastName,
    String address
) {}
