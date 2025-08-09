package com.midlane.project_management_tool_auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn; // in seconds
}
