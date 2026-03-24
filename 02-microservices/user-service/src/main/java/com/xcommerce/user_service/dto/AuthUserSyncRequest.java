package com.xcommerce.user_service.dto;

public record AuthUserSyncRequest(
    String username,
    String email,
    String passwordHash,
    String role,
    Boolean active,
    String firstName,
    String lastName,
    String address
) {}
