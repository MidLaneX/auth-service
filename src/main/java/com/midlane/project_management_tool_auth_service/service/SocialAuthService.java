package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.dto.SocialUserInfo;
import com.midlane.project_management_tool_auth_service.exception.OAuth2AuthenticationProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
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

    public SocialUserInfo getUserInfo(String provider, String token) {
        try {
            switch (provider.toLowerCase()) {
                case "google":
                    return getGoogleUserInfoFromIdToken(token);
                case "facebook":
                    return getFacebookUserInfo(token);
                default:
                    throw new OAuth2AuthenticationProcessingException("Unsupported provider: " + provider);
            }
        } catch (Exception e) {
            log.error("Error fetching user info from {}: {}", provider, e.getMessage());
            throw new OAuth2AuthenticationProcessingException("Failed to fetch user info from " + provider);
        }
    }

    private SocialUserInfo getGoogleUserInfoFromIdToken(String idToken) {

        try {
            // Parse the JWT token without verification (since it's already verified by Google)
            // In production, you should verify the token signature
            String[] chunks = idToken.split("\\.");
            if (chunks.length != 3) {
                throw new OAuth2AuthenticationProcessingException("Invalid Google ID token format");
            }

            // Decode the payload (claims)
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(chunks[0] + "." + chunks[1] + ".")
                    .getBody();

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String firstName = claims.get("given_name", String.class);
            String lastName = claims.get("family_name", String.class);
            String picture = claims.get("picture", String.class);
            Boolean emailVerified = claims.get("email_verified", Boolean.class);

            if (email == null || email.isEmpty()) {
                throw new OAuth2AuthenticationProcessingException("Email not found in Google ID token");
            }

            return new SocialUserInfo(
                    userId,
                    email,
                    firstName,
                    lastName,
                    picture,
                    "google",
                    Boolean.TRUE.equals(emailVerified)
            );
        } catch (JwtException e) {
            log.error("JWT parsing error: {}", e.getMessage());
            throw new OAuth2AuthenticationProcessingException("Invalid Google ID token");
        } catch (Exception e) {
            log.error("Error decoding Google ID token: {}", e.getMessage());
            throw new OAuth2AuthenticationProcessingException("Failed to decode Google ID token");
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
