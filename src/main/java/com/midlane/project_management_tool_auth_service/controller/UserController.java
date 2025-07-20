package com.midlane.project_management_tool_auth_service.controller;

import com.midlane.project_management_tool_auth_service.dto.UserDTO;
import com.midlane.project_management_tool_auth_service.exception.ErrorResponse;
import com.midlane.project_management_tool_auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/test")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint accessed successfully");
    }

    @PutMapping("/reset-password/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long userId,
            @RequestBody String newPassword) {
        try {
            userService.resetPassword(userId, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("RESET_PASSWORD_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
