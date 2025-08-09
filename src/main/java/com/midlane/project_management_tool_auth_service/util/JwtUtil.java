package com.midlane.project_management_tool_auth_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtUtil {

    private final RsaKeyUtil rsaKeyUtil;
    private final String privateKeyString;
    private final String publicKeyString;

    @Value("${jwt.access-token.expiration:900000}") // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    public JwtUtil(RsaKeyUtil rsaKeyUtil,
                   String privateKeyString,
                   @Qualifier("rsaPublicKey") String publicKeyString) {
        this.rsaKeyUtil = rsaKeyUtil;
        this.privateKeyString = privateKeyString;
        this.publicKeyString = publicKeyString;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public String extractTokenType(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("tokenType", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            PublicKey publicKey = rsaKeyUtil.decodePublicKey(publicKeyString);
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT token", e);
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String role = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));
        claims.put("role", role);
        claims.put("tokenType", "ACCESS");
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        try {
            log.debug("Creating JWT token for subject: {}", subject);
            PrivateKey privateKey = rsaKeyUtil.decodePrivateKey(privateKeyString);
            log.debug("Successfully decoded private key for token creation");

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            log.debug("Successfully created JWT token");
            return token;
        } catch (Exception e) {
            log.error("Failed to create JWT token for subject {}: {}", subject, e.getMessage(), e);
            throw new RuntimeException("Failed to create JWT token: " + e.getMessage(), e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "ACCESS".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "REFRESH".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String getPublicKey() {
        return publicKeyString;
    }
}
