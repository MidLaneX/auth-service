package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.AuthResponse;
import com.midlane.project_management_tool_auth_service.dto.RegisterRequest;
import com.midlane.project_management_tool_auth_service.model.User;
import com.midlane.project_management_tool_auth_service.repository.UserRepository;
import com.midlane.project_management_tool_auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse registerUser(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setPasswordLastChanged(LocalDateTime.now());
        user.setUserCreated(LocalDateTime.now());
        user.setEmailLastChanged(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .build();
    }
}
