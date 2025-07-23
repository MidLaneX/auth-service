package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.EmailVerificationRequest;
import com.midlane.project_management_tool_auth_service.dto.EmailVerificationResponse;
import com.midlane.project_management_tool_auth_service.model.EmailVerification;
import com.midlane.project_management_tool_auth_service.model.User;
import com.midlane.project_management_tool_auth_service.repository.EmailVerificationRepository;
import com.midlane.project_management_tool_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.email.verification.expiry-hours:24}")
    private int expiryHours;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(EmailVerificationRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Delete any existing unverified tokens for this user
            emailVerificationRepository.deleteUnverifiedTokensByUser(user);

            // Generate new verification token
            String token = UUID.randomUUID().toString();

            // Create verification record
            EmailVerification verification = new EmailVerification();
            verification.setUser(user);
            verification.setVerificationToken(token);
            verification.setTokenExpiry(LocalDateTime.now().plusHours(expiryHours));

            emailVerificationRepository.save(verification);

            // Send verification email
            String verificationLink = frontendUrl + "/verify-email?token=" + token;
            emailService.sendVerificationEmail(user.getEmail(), verificationLink);

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(String token) {
        try {
            EmailVerification verification = emailVerificationRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Invalid verification token"));

            // Check if token is expired
            if (verification.getTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification token has expired");
            }

            // Check if already verified
            if (verification.getVerifiedAt() != null) {
                return EmailVerificationResponse.builder()
                        .success(true)
                        .message("Email is already verified")
                        .email(verification.getUser().getEmail())
                        .verified(true)
                        .hasPendingToken(false)
                        .build();
            }

            // Mark as verified
            verification.setVerifiedAt(LocalDateTime.now());
            emailVerificationRepository.save(verification);

            // Update user's email verification status
            User user = verification.getUser();
            user.setEmailVerified(true);
            userRepository.save(user);

            // Send welcome email
            emailService.sendWelcomeEmail(user.getEmail());

            log.info("Email verified successfully for user: {}", user.getEmail());

            return EmailVerificationResponse.builder()
                    .success(true)
                    .message("Email verified successfully!")
                    .email(user.getEmail())
                    .verified(true)
                    .hasPendingToken(false)
                    .build();

        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            return EmailVerificationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .verified(false)
                    .hasPendingToken(false)
                    .build();
        }
    }

    public EmailVerificationResponse resendVerificationEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getEmailVerified()) {
                return EmailVerificationResponse.builder()
                        .success(false)
                        .message("Email is already verified")
                        .email(email)
                        .verified(true)
                        .hasPendingToken(false)
                        .build();
            }

            // Send new verification email
            EmailVerificationRequest request = EmailVerificationRequest.builder()
                    .email(email)
                    .build();
            sendVerificationEmail(request);

            return EmailVerificationResponse.builder()
                    .success(true)
                    .message("Verification email sent successfully")
                    .email(email)
                    .verified(false)
                    .hasPendingToken(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to resend verification email to: {}", email, e);
            return EmailVerificationResponse.builder()
                    .success(false)
                    .message("Failed to send verification email")
                    .email(email)
                    .verified(false)
                    .hasPendingToken(false)
                    .build();
        }
    }

    public EmailVerificationResponse checkVerificationStatus(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<EmailVerification> pendingVerification =
                    emailVerificationRepository.findByUserAndVerifiedAtIsNull(user);

            boolean hasPendingToken = pendingVerification.isPresent() &&
                    pendingVerification.get().getTokenExpiry().isAfter(LocalDateTime.now());

            return EmailVerificationResponse.builder()
                    .success(true)
                    .message("Status retrieved successfully")
                    .email(email)
                    .verified(user.getEmailVerified())
                    .hasPendingToken(hasPendingToken)
                    .build();

        } catch (Exception e) {
            log.error("Failed to check verification status for: {}", email, e);
            return EmailVerificationResponse.builder()
                    .success(false)
                    .message("Failed to retrieve status")
                    .email(email)
                    .verified(false)
                    .hasPendingToken(false)
                    .build();
        }
    }

    // Scheduled task to clean up expired tokens (runs every hour)
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            emailVerificationRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired verification tokens cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens", e);
        }
    }
}
