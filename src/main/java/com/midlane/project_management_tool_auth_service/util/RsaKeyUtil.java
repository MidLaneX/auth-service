package com.midlane.project_management_tool_auth_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class RsaKeyUtil {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generate RSA Key Pair
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Convert private key to Base64 string
     */
    public String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Convert public key to Base64 string
     */
    public String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Convert Base64 string to private key
     * Handles both clean Base64 strings and PEM format strings
     */
    public PrivateKey decodePrivateKey(String encodedKey) throws Exception {
        try {
            // Clean the key string - remove PEM headers/footers and whitespace
            String cleanKey = cleanKeyString(encodedKey);

            // Decode the Base64 string
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);

            // Create the key specification and generate the private key
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            PrivateKey privateKey = keyFactory.generatePrivate(spec);
            log.debug("Successfully decoded RSA private key");
            return privateKey;
        } catch (Exception e) {
            log.error("Failed to decode RSA private key: {}", e.getMessage());
            throw new Exception("Failed to decode RSA private key: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Base64 string to public key
     * Handles both clean Base64 strings and PEM format strings
     */
    public PublicKey decodePublicKey(String encodedKey) throws Exception {
        try {
            // Clean the key string - remove PEM headers/footers and whitespace
            String cleanKey = cleanKeyString(encodedKey);

            // Decode the Base64 string
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);

            // Create the key specification and generate the public key
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            PublicKey publicKey = keyFactory.generatePublic(spec);
            log.debug("Successfully decoded RSA public key");
            return publicKey;
        } catch (Exception e) {
            log.error("Failed to decode RSA public key: {}", e.getMessage());
            throw new Exception("Failed to decode RSA public key: " + e.getMessage(), e);
        }
    }

    /**
     * Clean key string by removing PEM headers/footers and all whitespace
     */
    private String cleanKeyString(String keyString) {
        return keyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "") // Remove all whitespace including newlines
                .trim();
    }
}
