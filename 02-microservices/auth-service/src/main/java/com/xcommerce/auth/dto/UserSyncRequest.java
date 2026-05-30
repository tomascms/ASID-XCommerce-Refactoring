package com.xcommerce.auth.dto;

public record UserSyncRequest(
    String username,
    String email,
    String passwordHash,
    String role,
    String status,
    Boolean active,
    String firstName,
    String lastName,
    String address
) {}
