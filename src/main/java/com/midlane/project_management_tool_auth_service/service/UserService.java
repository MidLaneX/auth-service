package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.AuthResponse;
import com.midlane.project_management_tool_auth_service.dto.LoginRequest;
import com.midlane.project_management_tool_auth_service.dto.RegisterRequest;
import com.midlane.project_management_tool_auth_service.dto.UserDTO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        // Authenticate user
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        );

        AuthenticationManager authenticationManager = new AuthenticationManager() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
                if (passwordEncoder.matches(authentication.getCredentials().toString(), userDetails.getPassword())) {
                    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                }
                throw new BadCredentialsException("Invalid credentials");
            }
        };

        authenticationManager.authenticate(authToken);

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        // Find User by email to get userId
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(userDetails.getUsername())
                .build();
    }
}
