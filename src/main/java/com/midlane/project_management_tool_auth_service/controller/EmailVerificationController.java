package com.midlane.project_management_tool_auth_service.controller;

import com.midlane.project_management_tool_auth_service.dto.EmailVerificationRequest;
import com.midlane.project_management_tool_auth_service.dto.EmailVerificationResponse;
import com.midlane.project_management_tool_auth_service.exception.ErrorResponse;
import com.midlane.project_management_tool_auth_service.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * Verify email using token sent via email
     * GET /api/auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            EmailVerificationResponse response = emailVerificationService.verifyEmail(token);
            log.info("Email verification successful for token: {}", token.substring(0, 10) + "...");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.error("Email verification failed for token: {}. Error: {}",
                     token.substring(0, 10) + "...", ex.getMessage());
            ErrorResponse error = new ErrorResponse("VERIFICATION_FAILED", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Resend verification email
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody EmailVerificationRequest request) {
        try {
            EmailVerificationResponse response = emailVerificationService.resendVerificationEmail(request.getEmail());
            log.info("Verification email resent successfully to: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.error("Failed to resend verification email to: {}. Error: {}",
                     request.getEmail(), ex.getMessage());
            ErrorResponse error = new ErrorResponse("RESEND_FAILED", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Check verification status
     * GET /api/auth/verification-status/{email}
     */
    @GetMapping("/verification-status/{email}")
    public ResponseEntity<?> checkVerificationStatus(@PathVariable String email) {
        try {
            boolean isVerified = emailVerificationService.isEmailVerified(email);
            EmailVerificationResponse response = EmailVerificationResponse.builder()
                    .success(true)
                    .message(isVerified ? "Email is verified" : "Email is not verified")
                    .verified(isVerified)
                    .build();
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.error("Failed to check verification status for: {}. Error: {}", email, ex.getMessage());
            ErrorResponse error = new ErrorResponse("STATUS_CHECK_FAILED", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
