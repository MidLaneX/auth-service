package com.midlane.project_management_tool_auth_service.dto;

import com.midlane.project_management_tool_auth_service.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long userId;
    private String email;
    private String phone;
    private Enum<Role> role;
    private LocalDateTime userCreated;
}
