package com.xcommerce.user_service.dto;

public record UserCreatedEvent(
    String username,
    String email,
    String passwordHash,
    String role,
    String firstName,
    String lastName,
    String address
) {}