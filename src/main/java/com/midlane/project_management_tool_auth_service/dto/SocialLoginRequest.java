package com.midlane.project_management_tool_auth_service.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotBlank(message = "Provider is required")
    private String provider; // "google" or "facebook"
}
