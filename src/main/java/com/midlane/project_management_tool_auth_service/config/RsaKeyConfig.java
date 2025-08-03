package com.midlane.project_management_tool_auth_service.config;

import com.midlane.project_management_tool_auth_service.util.RsaKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.security.KeyPair;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RsaKeyConfig {

    private final RsaKeyUtil rsaKeyUtil;

    @Value("${RSA_PRIVATE_KEY:}")
    private String privateKeyEnv;

    @Value("${RSA_PUBLIC_KEY:}")
    private String publicKeyEnv;

    @Value("${RSA_KEY_STORE_PATH:./keys}")
    private String keyStorePath;

    @Bean
    @Primary
    public String rsaPrivateKey() {
        try {
            // First try to get from environment variable
            if (!privateKeyEnv.isEmpty()) {
                log.info("Using RSA private key from environment variable");
                return privateKeyEnv;
            }

            // Try to read from file, if it doesn't exist, generate new keys
            try {
                log.info("Attempting to read RSA private key from file");
                return getPrivateKeyFromFile();
            } catch (Exception e) {
                log.warn("Could not read RSA private key from file: {}", e.getMessage());

                // Only generate and save if we can create the directory
                if (canCreateKeyDirectory()) {
                    log.info("Generating new key pair and saving to files");
                    return generateAndSaveKeyPair().getPrivateKey();
                } else {
                    log.info("Cannot write to file system, generating in-memory key pair");
                    return generateInMemoryKeyPair().getPrivateKey();
                }
            }
        } catch (Exception e) {
            log.error("Failed to load RSA private key", e);
            throw new RuntimeException("Failed to load RSA private key", e);
        }
    }

    @Bean
    public String rsaPublicKey() {
        try {
            // First try to get from environment variable
            if (!publicKeyEnv.isEmpty()) {
                log.info("Using RSA public key from environment variable");
                return publicKeyEnv;
            }

            // Try to read from file, if it doesn't exist, generate new keys
            try {
                log.info("Attempting to read RSA public key from file");
                return getPublicKeyFromFile();
            } catch (Exception e) {
                log.warn("Could not read RSA public key from file: {}", e.getMessage());

                // Only generate and save if we can create the directory
                if (canCreateKeyDirectory()) {
                    log.info("Generating new key pair and saving to files");
                    return generateAndSaveKeyPair().getPublicKey();
                } else {
                    log.info("Cannot write to file system, generating in-memory key pair");
                    return generateInMemoryKeyPair().getPublicKey();
                }
            }
        } catch (Exception e) {
            log.error("Failed to load RSA public key", e);
            throw new RuntimeException("Failed to load RSA public key", e);
        }
    }

    private boolean canCreateKeyDirectory() {
        try {
            java.io.File keyDir = new java.io.File(keyStorePath);
            if (!keyDir.exists()) {
                boolean created = keyDir.mkdirs();
                if (!created) {
                    log.warn("Could not create key directory: {}", keyStorePath);
                    return false;
                }
            }

            // Test if we can write to the directory
            java.io.File testFile = new java.io.File(keyDir, "test.tmp");
            try (java.io.FileWriter writer = new java.io.FileWriter(testFile)) {
                writer.write("test");
            }
            testFile.delete();
            return true;
        } catch (Exception e) {
            log.warn("Cannot write to key directory {}: {}", keyStorePath, e.getMessage());
            return false;
        }
    }

    private KeyPairData generateInMemoryKeyPair() {
        try {
            log.info("Generating in-memory RSA key pair...");
            KeyPair keyPair = rsaKeyUtil.generateKeyPair();

            String privateKeyPem = rsaKeyUtil.encodePrivateKey(keyPair.getPrivate());
            String publicKeyPem = rsaKeyUtil.encodePublicKey(keyPair.getPublic());

            log.info("RSA key pair generated in memory successfully");
            log.warn("Keys are stored in memory only and will be regenerated on restart");
            log.info("Consider setting RSA_PRIVATE_KEY and RSA_PUBLIC_KEY environment variables for persistence");

            return new KeyPairData(privateKeyPem, publicKeyPem);
        } catch (Exception e) {
            log.error("Failed to generate in-memory RSA key pair", e);
            throw new RuntimeException("Failed to generate in-memory RSA key pair", e);
        }
    }

    private KeyPairData generateAndSaveKeyPair() {
        try {
            log.info("Generating new RSA key pair...");

            KeyPair keyPair = rsaKeyUtil.generateKeyPair();

            String privateKeyPem = rsaKeyUtil.encodePrivateKey(keyPair.getPrivate());
            String publicKeyPem = rsaKeyUtil.encodePublicKey(keyPair.getPublic());

            // Save private key
            java.io.File privateKeyFile = new java.io.File(keyStorePath + "/private_key.pem");
            try (java.io.FileWriter writer = new java.io.FileWriter(privateKeyFile)) {
                writer.write("-----BEGIN PRIVATE KEY-----\n");
                writer.write(privateKeyPem);
                writer.write("\n-----END PRIVATE KEY-----\n");
            }

            // Save public key
            java.io.File publicKeyFile = new java.io.File(keyStorePath + "/public_key.pem");
            try (java.io.FileWriter writer = new java.io.FileWriter(publicKeyFile)) {
                writer.write("-----BEGIN PUBLIC KEY-----\n");
                writer.write(publicKeyPem);
                writer.write("\n-----END PUBLIC KEY-----\n");
            }

            log.info("RSA key pair generated and saved successfully at: {}", keyStorePath);
            log.info("Public key can be shared with other services for token verification");

            return new KeyPairData(privateKeyPem, publicKeyPem);
        } catch (Exception e) {
            log.error("Failed to generate and save RSA key pair", e);
            throw new RuntimeException("Failed to generate and save RSA key pair", e);
        }
    }

    private String getPublicKeyFromFile() throws java.io.IOException {
        String publicKeyPath = keyStorePath + "/public_key.pem";
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(publicKeyPath)));
        return content.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s", "");
    }

    private String getPrivateKeyFromFile() throws java.io.IOException {
        String privateKeyPath = keyStorePath + "/private_key.pem";
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(privateKeyPath)));
        return content.replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", "")
                      .replaceAll("\\s", "");
    }

    private static class KeyPairData {
        private final String privateKey;
        private final String publicKey;

        public KeyPairData(String privateKey, String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }
    }
}
