package com.midlane.project_management_tool_auth_service.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false)
    private Long userId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash; // Made nullable for social login users

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER; // Default to USER

    @Column(name = "password_last_changed")
    private LocalDateTime passwordLastChanged;

    @CreationTimestamp
    @Column(name = "user_created", nullable = false, updatable = false)
    private LocalDateTime userCreated;

    @Column(name = "email_last_changed")
    private LocalDateTime emailLastChanged;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false; // Default to false

    // Social login fields
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private AuthProvider provider = AuthProvider.LOCAL; // Default to local

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
}