package com.midlane.project_management_tool_auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    private String provider; // "google", "facebook", etc.

    @JsonProperty("accessToken")
    private String accessToken; // Token from the social provider
    private String email; // Optional, can be used for email-based login
    private String name; // Optional, can be used for name-based login

    @JsonProperty("ProfilePicture")
    private String profilePicture;
}

