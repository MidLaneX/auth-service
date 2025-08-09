package com.midlane.project_management_tool_auth_service.repository;

import com.midlane.project_management_tool_auth_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserEmailAndRevokedFalse(String userEmail);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userEmail = :userEmail")
    void revokeAllUserTokens(String userEmail);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    void revokeToken(String token);
}
