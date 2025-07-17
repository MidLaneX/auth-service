package com.midlane.project_management_tool_auth_service.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_providers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id", updatable = false, nullable = false)
    private Long oauthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProviderType provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "access_token", nullable = false, length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "scope")
    private String scope;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}