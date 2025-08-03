package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.model.RefreshToken;
import com.midlane.project_management_tool_auth_service.repository.RefreshTokenRepository;
import com.midlane.project_management_tool_auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpiration;

    @Value("${jwt.max-refresh-tokens-per-user:5}")
    private int maxRefreshTokensPerUser;

    @Transactional
    public RefreshToken createRefreshToken(UserDetails userDetails, String deviceInfo) {
        // Clean up old tokens if user has too many
        cleanupOldUserTokens(userDetails.getUsername());

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .userEmail(userDetails.getUsername())
                .expiresAt(expiryDate)
                .deviceInfo(deviceInfo)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired() || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token is expired or revoked. Please login again.");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeToken(token);
    }

    @Transactional
    public void revokeAllUserTokens(String userEmail) {
        refreshTokenRepository.revokeAllUserTokens(userEmail);
    }

    private void cleanupOldUserTokens(String userEmail) {
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserEmailAndRevokedFalse(userEmail);

        if (userTokens.size() >= maxRefreshTokensPerUser) {
            // Sort by creation date and remove oldest tokens
            userTokens.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

            int tokensToRemove = userTokens.size() - maxRefreshTokensPerUser + 1;
            for (int i = 0; i < tokensToRemove; i++) {
                RefreshToken oldToken = userTokens.get(i);
                oldToken.setRevoked(true);
                refreshTokenRepository.save(oldToken);
            }
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredTokens(now);
        log.info("Cleaned up expired refresh tokens");
    }

    public String generateAccessTokenFromRefreshToken(RefreshToken refreshToken, UserDetails userDetails) {
        return jwtUtil.generateAccessToken(userDetails);
    }
}
