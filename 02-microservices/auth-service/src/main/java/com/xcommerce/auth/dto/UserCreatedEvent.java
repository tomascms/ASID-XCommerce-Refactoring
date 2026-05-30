package com.xcommerce.auth.dto;

public record UserCreatedEvent(
    String username,
    String email,
    String passwordHash,
    String role,
    String firstName,
    String lastName,
    String address
) {}