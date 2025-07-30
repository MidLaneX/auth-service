package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.AuthResponse;
import com.midlane.project_management_tool_auth_service.dto.LoginRequest;
import com.midlane.project_management_tool_auth_service.dto.RegisterRequest;
import com.midlane.project_management_tool_auth_service.dto.UserDTO;
import com.midlane.project_management_tool_auth_service.dto.EmailVerificationRequest;
import com.midlane.project_management_tool_auth_service.dto.SocialLoginRequest;
import com.midlane.project_management_tool_auth_service.dto.SocialUserInfo;
import com.midlane.project_management_tool_auth_service.model.AuthProvider;
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
    private final SocialAuthService socialAuthService;

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Encode the new password and update user
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        user.setPasswordLastChanged(LocalDateTime.now());

        userRepository.save(user);

        return "Password reset successfully";
    }

    public void updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update role
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse authenticateWithSocial(SocialLoginRequest request) {
        // Log for error tracking
        System.out.println("Social login request: " + request);

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
            }
        } else {
            // Create new user from social login
            user = createUserFromSocialInfo(socialUserInfo);
            isNewUser = true;
        }

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        String message = isNewUser ?
            "Registration successful via " + request.getProvider() + "!" :
            "Login successful via " + request.getProvider() + "!";

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .message(message)
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
        user.setRole(Role.USER);
        user.setEmailVerified(socialUserInfo.isEmailVerified());
        user.setUserCreated(LocalDateTime.now());

        return userRepository.save(user);
    }
}
