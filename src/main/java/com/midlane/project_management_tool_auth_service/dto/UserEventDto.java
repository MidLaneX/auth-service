package com.midlane.project_management_tool_auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDto {
    private Long userId;
    private String email;
    private String eventType; // e.g., "USER_CREATED", "USER_UPDATED", "USER_DELETED"
}
