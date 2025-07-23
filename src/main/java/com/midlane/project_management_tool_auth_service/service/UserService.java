package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.AuthResponse;
import com.midlane.project_management_tool_auth_service.dto.LoginRequest;
import com.midlane.project_management_tool_auth_service.dto.RegisterRequest;
import com.midlane.project_management_tool_auth_service.dto.UserDTO;
import com.midlane.project_management_tool_auth_service.dto.EmailVerificationRequest;
import com.midlane.project_management_tool_auth_service.model.Role;
import com.midlane.project_management_tool_auth_service.model.User;
import com.midlane.project_management_tool_auth_service.repository.UserRepository;
import com.midlane.project_management_tool_auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailVerificationService emailVerificationService;

    public AuthResponse registerUser(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user with email verification disabled by default
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.USER); // Default to USER role
        user.setEmailVerified(false); // Email not verified initially
        user.setPasswordLastChanged(LocalDateTime.now());
        user.setUserCreated(LocalDateTime.now());
        user.setEmailLastChanged(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Send verification email asynchronously
        EmailVerificationRequest verificationRequest = EmailVerificationRequest.builder()
            .email(savedUser.getEmail())
            .build();
        emailVerificationService.sendVerificationEmail(verificationRequest);

        // Generate JWT token (user can access limited features until email is verified)
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .emailVerified(savedUser.getEmailVerified())
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userCreated(user.getUserCreated())
                .build();
    }

    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Use the properly configured AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Find User by email to get userId and role
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate JWT token
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String token = jwtUtil.generateToken(userDetails);

            String message = user.getEmailVerified() ?
                "Login successful!" :
                "Login successful! Please verify your email to access all features.";

            return AuthResponse.builder()
                    .token(token)
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .emailVerified(user.getEmailVerified())
                    .message(message)
                    .build();

        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public String resetPassword(Long userId, String newPassword) {
        System.out.println("=== PASSWORD RESET DEBUG ===");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("Original hash: " + user.getPasswordHash());

        String encodedPassword = passwordEncoder.encode(newPassword);
        System.out.println("New encoded password: " + encodedPassword);

        user.setPasswordHash(encodedPassword);
        user.setPasswordLastChanged(LocalDateTime.now());

        User savedUser = userRepository.saveAndFlush(user);
        System.out.println("Returned saved hash: " + savedUser.getPasswordHash());

        // Query database directly
        User dbUser = userRepository.findById(userId).get();
        System.out.println("Database queried hash: " + dbUser.getPasswordHash());

        System.out.println("Encoded == Saved: " + encodedPassword.equals(savedUser.getPasswordHash()));
        System.out.println("Encoded == DB: " + encodedPassword.equals(dbUser.getPasswordHash()));

        return "Password reset successfully";
    }

    public void updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update role
        user.setRole(role);
        userRepository.save(user);
    }
}
