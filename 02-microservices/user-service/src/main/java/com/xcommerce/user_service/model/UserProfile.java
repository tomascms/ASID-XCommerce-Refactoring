package com.xcommerce.user_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private Long id; // ID sincronizado com o Auth Service
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private String phoneNumber;
}