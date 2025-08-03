package com.midlane.project_management_tool_auth_service.controller;

import com.midlane.project_management_tool_auth_service.dto.*;
import com.midlane.project_management_tool_auth_service.exception.ErrorResponse;
import com.midlane.project_management_tool_auth_service.exception.OAuth2AuthenticationProcessingException;
import com.midlane.project_management_tool_auth_service.service.RefreshTokenService;
import com.midlane.project_management_tool_auth_service.service.UserService;
import com.midlane.project_management_tool_auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/initial")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String deviceInfo = extractDeviceInfo(httpRequest);
        return ResponseEntity.ok(userService.registerUser(request, deviceInfo));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String deviceInfo = extractDeviceInfo(httpRequest);
            return ResponseEntity.ok(userService.loginUser(request, deviceInfo));
        } catch (BadCredentialsException ex) {
            ErrorResponse error = new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("LOGIN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            RefreshTokenResponse response = userService.refreshAccessToken(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("REFRESH_TOKEN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        try {
            refreshTokenService.revokeToken(request.getRefreshToken());
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("LOGOUT_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices(@RequestParam String userEmail) {
        try {
            refreshTokenService.revokeAllUserTokens(userEmail);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("LOGOUT_ALL_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        PublicKeyResponse response = new PublicKeyResponse(jwtUtil.getPublicKey(), "RS256", "RSA");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/login")
    public ResponseEntity<?> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        try {
            AuthResponse response = userService.authenticateWithSocial(request);
            return ResponseEntity.ok(response);
        } catch (OAuth2AuthenticationProcessingException ex) {
            ErrorResponse error = new ErrorResponse("SOCIAL_AUTH_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("SOCIAL_LOGIN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        return String.format("%s - %s", userAgent != null ? userAgent : "Unknown", remoteAddr);
    }
}
