package com.midlane.project_management_tool_auth_service.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "mfa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mfa_id", updatable = false, nullable = false)
    private Long mfaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", nullable = false)
    private MFAMethod mfaMethod;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_verified", nullable = false)
    private Boolean mfaVerified = false;

    @Column(name = "mfa_last_used")
    private LocalDateTime mfaLastUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "backup_codes")
    private String backupCodes;
}