package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.model.User;
import com.midlane.project_management_tool_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Handle null password for social login users
        String password = user.getPasswordHash();
        if (password == null) {
            // For social login users, set a placeholder password since they don't authenticate with passwords
            password = "{noop}"; // No-op password encoder prefix for empty password
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(password)
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
