package com.midlane.project_management_tool_auth_service.controller;

import com.midlane.project_management_tool_auth_service.dto.AuthResponse;
import com.midlane.project_management_tool_auth_service.dto.LoginRequest;
import com.midlane.project_management_tool_auth_service.dto.RegisterRequest;
import com.midlane.project_management_tool_auth_service.dto.SocialLoginRequest;
import com.midlane.project_management_tool_auth_service.dto.UserDTO;
import com.midlane.project_management_tool_auth_service.exception.ErrorResponse;
import com.midlane.project_management_tool_auth_service.exception.OAuth2AuthenticationProcessingException;
import com.midlane.project_management_tool_auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/auth/initial")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(userService.loginUser(request));
        } catch (BadCredentialsException ex) {
            ErrorResponse error = new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("LOGIN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
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

    @PostMapping("/social/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody SocialLoginRequest request) {
        try {
            request.setProvider("google");
            AuthResponse response = userService.authenticateWithSocial(request);
            return ResponseEntity.ok(response);
        } catch (OAuth2AuthenticationProcessingException ex) {
            ErrorResponse error = new ErrorResponse("GOOGLE_AUTH_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("GOOGLE_LOGIN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/social/facebook")
    public ResponseEntity<?> facebookLogin(@Valid @RequestBody SocialLoginRequest request) {
        try {
            request.setProvider("facebook");
            AuthResponse response = userService.authenticateWithSocial(request);
            return ResponseEntity.ok(response);
        } catch (OAuth2AuthenticationProcessingException ex) {
            ErrorResponse error = new ErrorResponse("FACEBOOK_AUTH_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("FACEBOOK_LOGIN_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
