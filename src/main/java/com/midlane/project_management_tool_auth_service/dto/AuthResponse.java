package com.midlane.project_management_tool_auth_service.dto;

import com.midlane.project_management_tool_auth_service.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private Enum<Role> role;
}
