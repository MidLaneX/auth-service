package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final RestTemplate restTemplate;

    @Value("${app.oauth.google.client-id}")
    private String googleClientId;

    public SocialUserInfo getUserInfo(String provider, String accessToken) {
        switch (provider.toLowerCase()) {
            case "google":
                return getGoogleUserInfo(accessToken);
            case "facebook":
                return getFacebookUserInfo(accessToken);
            default:
                throw new RuntimeException("Unsupported social provider: " + provider);
        }
    }

    private SocialUserInfo getGoogleUserInfo(String token) {
        try {
            // Check if this is an ID token (JWT format) or access token
            if (isJwtToken(token)) {
                return getGoogleUserInfoFromIdToken(token);
            } else {
                return getGoogleUserInfoFromAccessToken(token);
            }
        } catch (Exception e) {
            log.error("Error getting Google user info", e);
            throw new RuntimeException("Failed to get user info from Google: " + e.getMessage());
        }
    }

    private boolean isJwtToken(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    private SocialUserInfo getGoogleUserInfoFromIdToken(String idToken) {
        try {
            log.debug("Processing Google ID token directly");

            // Verify the ID token with Google
            String verifyUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken;

            ResponseEntity<Map> response = restTemplate.getForEntity(verifyUrl, Map.class);
            Map<String, Object> userInfo = response.getBody();

            if (userInfo == null || userInfo.containsKey("error")) {
                String error = userInfo != null ? (String) userInfo.get("error_description") : "Unknown error";
                throw new RuntimeException("Invalid Google ID token: " + error);
            }

            // Verify the audience (client ID)
            String audience = (String) userInfo.get("aud");
            if (!googleClientId.equals(audience)) {
                throw new RuntimeException("ID token audience does not match client ID");
            }

            log.debug("Successfully verified Google ID token for email: {}", userInfo.get("email"));

            return new SocialUserInfo(
                (String) userInfo.get("sub"), // Google's user ID
                (String) userInfo.get("email"),
                (String) userInfo.get("given_name"),
                (String) userInfo.get("family_name"),
                (String) userInfo.get("picture"),
                "google",
                Boolean.TRUE.equals(userInfo.get("email_verified"))
            );

        } catch (Exception e) {
            log.error("Error processing Google ID token", e);
            throw new RuntimeException("Failed to process Google ID token: " + e.getMessage());
        }
    }

    private SocialUserInfo getGoogleUserInfoFromAccessToken(String accessToken) {
        try {
            log.debug("Processing Google access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null) {
                throw new RuntimeException("Failed to get user info from Google");
            }

            log.debug("Successfully retrieved Google user info for email: {}", userInfo.get("email"));

            return new SocialUserInfo(
                (String) userInfo.get("id"),
                (String) userInfo.get("email"),
                (String) userInfo.get("given_name"),
                (String) userInfo.get("family_name"),
                (String) userInfo.get("picture"),
                "google",
                Boolean.TRUE.equals(userInfo.get("verified_email"))
            );
        } catch (Exception e) {
            log.error("Error getting Google user info from access token", e);
            throw new RuntimeException("Failed to get user info from Google access token: " + e.getMessage());
        }
    }

    private SocialUserInfo getFacebookUserInfo(String accessToken) {
        try {
            String url = "https://graph.facebook.com/me?fields=id,email,first_name,last_name,picture&access_token=" + accessToken;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null) {
                throw new RuntimeException("Failed to get user info from Facebook");
            }

            Map<String, Object> picture = (Map<String, Object>) userInfo.get("picture");
            Map<String, Object> pictureData = picture != null ? (Map<String, Object>) picture.get("data") : null;
            String pictureUrl = pictureData != null ? (String) pictureData.get("url") : null;

            return new SocialUserInfo(
                (String) userInfo.get("id"),
                (String) userInfo.get("email"),
                (String) userInfo.get("first_name"),
                (String) userInfo.get("last_name"),
                pictureUrl,
                "facebook",
                userInfo.get("email") != null // Facebook email is considered verified if provided
            );
        } catch (Exception e) {
            log.error("Error getting Facebook user info", e);
            throw new RuntimeException("Failed to get user info from Facebook: " + e.getMessage());
        }
    }
}
