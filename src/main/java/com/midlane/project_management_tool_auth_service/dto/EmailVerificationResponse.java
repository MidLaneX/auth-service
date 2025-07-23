package com.midlane.project_management_tool_auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationResponse {
    private String message;
    private boolean success;
    private String email;
    private boolean verified;
    private boolean hasPendingToken;
}
