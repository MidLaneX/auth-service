package com.midlane.project_management_tool_auth_service.repository;

import com.midlane.project_management_tool_auth_service.model.EmailVerification;
import com.midlane.project_management_tool_auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByVerificationToken(String verificationToken);

    Optional<EmailVerification> findByUserAndVerifiedAtIsNull(User user);

    boolean existsByVerificationToken(String verificationToken);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification ev WHERE ev.tokenExpiry < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification ev WHERE ev.user = :user AND ev.verifiedAt IS NULL")
    void deleteUnverifiedTokensByUser(@Param("user") User user);

    @Query("SELECT COUNT(ev) FROM EmailVerification ev WHERE ev.user = :user AND ev.verifiedAt IS NOT NULL")
    long countVerifiedTokensByUser(@Param("user") User user);
}
