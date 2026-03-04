package com.xcommerce.auth.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users_micro") // Nome diferente para não chocar com o monólito
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;
    
    private String password;
    private String role;
}