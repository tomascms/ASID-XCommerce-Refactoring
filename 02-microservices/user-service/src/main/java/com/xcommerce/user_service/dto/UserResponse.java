package com.xcommerce.user_service.dto;

import com.xcommerce.user_service.model.User;

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
