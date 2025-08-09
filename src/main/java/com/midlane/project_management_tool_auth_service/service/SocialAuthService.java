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

    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        try {
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
            log.error("Error getting Google user info", e);
            throw new RuntimeException("Failed to get user info from Google: " + e.getMessage());
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
