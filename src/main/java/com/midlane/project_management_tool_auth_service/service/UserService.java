package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.*;
import com.midlane.project_management_tool_auth_service.model.AuthProvider;
import com.midlane.project_management_tool_auth_service.model.RefreshToken;
import com.midlane.project_management_tool_auth_service.model.Role;
import com.midlane.project_management_tool_auth_service.model.User;
import com.midlane.project_management_tool_auth_service.repository.UserRepository;
import com.midlane.project_management_tool_auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;
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
    private final RefreshTokenService refreshTokenService;
    private final UserEventService userEventService;
    private final SocialAuthService socialAuthService;

    @Value("${jwt.access-token.expiration:900000}") // 15 minutes
    private long accessTokenExpiration;

    public AuthResponse registerUser(RegisterRequest request, String deviceInfo) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.USER); // Default to USER role
        user.setEmailVerified(false); // Email not verified initially
        user.setPasswordLastChanged(LocalDateTime.now());
        user.setEmailLastChanged(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Publish user registration event to Kafka
        userEventService.publishUserEvent(savedUser, "USER_CREATED");

        // Send verification email asynchronously
        EmailVerificationRequest verificationRequest = EmailVerificationRequest.builder()
            .email(savedUser.getEmail())
            .build();
        emailVerificationService.sendVerificationEmail(verificationRequest);

        // Generate tokens using RSA
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails, deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .userEmail(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse loginUser(LoginRequest request, String deviceInfo) {
        try {
            // Use the properly configured AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Find User by email to get userId and role
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate tokens using RSA
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String accessToken = jwtUtil.generateAccessToken(userDetails);

            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails, deviceInfo);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                    .userEmail(user.getEmail())
                    .role(user.getRole().name())
                    .build();

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(refreshToken.getUserEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
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
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Encode the new password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordLastChanged(LocalDateTime.now());

        // Save the updated user
        userRepository.save(user);

        // Publish user updated event to Kafka
        userEventService.publishUserEvent(user, "USER_UPDATED");

        // Revoke all refresh tokens for security after password change
        refreshTokenService.revokeAllUserTokens(user.getEmail());
    }

    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordLastChanged(LocalDateTime.now());

        // Save the updated user
        userRepository.save(user);

        // Publish user updated event to Kafka
        userEventService.publishUserEvent(user, "USER_UPDATED");

        // Revoke all refresh tokens for security after password change
        refreshTokenService.revokeAllUserTokens(user.getEmail());
    }

    @Transactional
    public void updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setRole(newRole);

        // Save the updated user
        userRepository.save(user);

        // Publish user updated event to Kafka
        userEventService.publishUserEvent(user, "USER_UPDATED");

        // Revoke all refresh tokens when role changes for security
        // This forces the user to log in again to get tokens with updated role claims
        refreshTokenService.revokeAllUserTokens(user.getEmail());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Publish user deleted event to Kafka before deletion
        userEventService.publishUserEvent(user, "USER_DELETED");

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user.getEmail());

        // Delete user
        userRepository.delete(user);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public AuthResponse authenticateWithSocial(SocialLoginRequest request) {
        // Get user info from social provider
        SocialUserInfo socialUserInfo = socialAuthService.getUserInfo(request.getProvider(), request.getAccessToken());

        if (socialUserInfo.getEmail() == null || socialUserInfo.getEmail().isEmpty()) {
            throw new RuntimeException("Email not provided by " + request.getProvider() + " provider");
        }

        // Check if user exists by email
        Optional<User> existingUser = userRepository.findByEmail(socialUserInfo.getEmail());

        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update social provider info if it's a local account being linked
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.valueOf(socialUserInfo.getProvider().toUpperCase()));
                user.setProviderId(socialUserInfo.getId());
                user.setFirstName(socialUserInfo.getFirstName());
                user.setLastName(socialUserInfo.getLastName());
                user.setProfilePictureUrl(socialUserInfo.getProfilePictureUrl());
                if (socialUserInfo.isEmailVerified()) {
                    user.setEmailVerified(true);
                }
                userRepository.save(user);

                // Publish user updated event to Kafka (minimal info)
                userEventService.publishUserEvent(user, "USER_UPDATED");
            }
        } else {
            // Create new user from social login
            user = createUserFromSocialInfo(socialUserInfo);
            isNewUser = true;

            // Publish user created event to Kafka (minimal info)
            userEventService.publishUserEvent(user, "USER_CREATED");
        }

        // Generate RSA-based tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        // Create refresh token with default device info for social login
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails, "Social Login - " + request.getProvider());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .userEmail(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private User createUserFromSocialInfo(SocialUserInfo socialUserInfo) {
        User user = new User();
        user.setEmail(socialUserInfo.getEmail());
        user.setFirstName(socialUserInfo.getFirstName());
        user.setLastName(socialUserInfo.getLastName());
        user.setProfilePictureUrl(socialUserInfo.getProfilePictureUrl());
        user.setProvider(AuthProvider.valueOf(socialUserInfo.getProvider().toUpperCase()));
        user.setProviderId(socialUserInfo.getId());
        user.setPasswordHash(null); // No password for social login
        user.setRole(Role.USER);
        user.setEmailVerified(socialUserInfo.isEmailVerified());
        user.setEmailLastChanged(LocalDateTime.now());

        return userRepository.save(user);
    }
}
