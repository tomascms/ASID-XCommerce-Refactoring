package com.xcommerce.identity.dto;

import com.xcommerce.identity.model.User;

public record UserResponse(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String address,
    String role,
    Boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            user.getRole(),
            user.getActive()
        );
    }
}
