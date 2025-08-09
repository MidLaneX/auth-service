package com.midlane.project_management_tool_auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private String provider;
    private boolean emailVerified;
}
