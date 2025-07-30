package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.SocialUserInfo;
import com.midlane.project_management_tool_auth_service.exception.OAuth2AuthenticationProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final WebClient.Builder webClientBuilder;

    public SocialUserInfo getUserInfo(String provider, String accessToken) {
        System.out.println("Fetching user info from provider: " + provider);
        System.out.println("Access Token: " + accessToken);
        try {
            switch (provider.toLowerCase()) {
                case "google":
                    return getGoogleUserInfo(accessToken);
                case "facebook":
                    return getFacebookUserInfo(accessToken);
                default:
                    throw new OAuth2AuthenticationProcessingException("Unsupported provider: " + provider);
            }
        } catch (Exception e) {
            log.error("Error fetching user info from {}: {}", provider, e.getMessage());
            throw new OAuth2AuthenticationProcessingException("Failed to fetch user info from " + provider);
        }
    }

    private SocialUserInfo getGoogleUserInfo(String accessToken) {
        try {
            WebClient webClient = webClientBuilder.build();

            Map<String, Object> userInfo = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (userInfo == null) {
                throw new OAuth2AuthenticationProcessingException("Failed to fetch user info from Google");
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
        } catch (WebClientResponseException e) {
            log.error("Google API error: {}", e.getResponseBodyAsString());
            throw new OAuth2AuthenticationProcessingException("Invalid Google access token");
        }
    }

    private SocialUserInfo getFacebookUserInfo(String accessToken) {
        try {
            WebClient webClient = webClientBuilder.build();

            Map<String, Object> userInfo = webClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,email,first_name,last_name,picture.type(large)")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (userInfo == null) {
                throw new OAuth2AuthenticationProcessingException("Failed to fetch user info from Facebook");
            }

            String profilePictureUrl = null;
            if (userInfo.get("picture") instanceof Map) {
                Map<String, Object> picture = (Map<String, Object>) userInfo.get("picture");
                if (picture.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    profilePictureUrl = (String) data.get("url");
                }
            }

            return new SocialUserInfo(
                    (String) userInfo.get("id"),
                    (String) userInfo.get("email"),
                    (String) userInfo.get("first_name"),
                    (String) userInfo.get("last_name"),
                    profilePictureUrl,
                    "facebook",
                    userInfo.get("email") != null // Facebook email is verified if present
            );
        } catch (WebClientResponseException e) {
            log.error("Facebook API error: {}", e.getResponseBodyAsString());
            throw new OAuth2AuthenticationProcessingException("Invalid Facebook access token");
        }
    }
}
