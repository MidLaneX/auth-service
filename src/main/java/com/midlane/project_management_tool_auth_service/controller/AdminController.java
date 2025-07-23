package com.midlane.project_management_tool_auth_service.controller;

import com.midlane.project_management_tool_auth_service.dto.UserDTO;
import com.midlane.project_management_tool_auth_service.model.Role;
import com.midlane.project_management_tool_auth_service.service.UserService;
import com.midlane.project_management_tool_auth_service.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testAdminEndpoint() {
        return ResponseEntity.ok("Admin endpoint accessed successfully");
    }

    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long userId,
            @RequestBody String newPassword) {
        // Logging of sensitive information has been removed for security reasons.
        try {
            userService.resetPassword(userId, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Error resetting password: " + ex.getMessage());
        }
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestParam Role role) {
        try {
            userService.updateUserRole(userId, role);
            return ResponseEntity.ok("User role updated successfully");
        } catch (RuntimeException ex) {
            ErrorResponse error = new ErrorResponse("UPDATE_ROLE_ERROR", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
