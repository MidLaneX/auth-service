package com.midlane.project_management_tool_auth_service.service;

import com.midlane.project_management_tool_auth_service.util.RsaKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;

@Service
@RequiredArgsConstructor
@Slf4j
public class RsaKeyGenerationService implements CommandLineRunner {

    private final RsaKeyUtil rsaKeyUtil;

    @Value("${rsa.key-store-path:./keys}")
    private String keyStorePath;

    @Override
    public void run(String... args) throws Exception {
        generateKeysIfNotExists();
    }

    private void generateKeysIfNotExists() {
        try {
            File keyDir = new File(keyStorePath);

            // Ensure the directory exists and is writable
            if (!keyDir.exists()) {
                log.info("Creating keys directory: {}", keyStorePath);
                boolean created = keyDir.mkdirs();
                if (!created) {
                    log.error("Failed to create keys directory: {}", keyStorePath);
                    throw new RuntimeException("Failed to create keys directory: " + keyStorePath);
                }
            }

            // Verify directory is writable
            if (!keyDir.canWrite()) {
                log.error("Keys directory is not writable: {}", keyStorePath);
                throw new RuntimeException("Keys directory is not writable: " + keyStorePath);
            }

            File privateKeyFile = new File(keyStorePath + "/private_key.pem");
            File publicKeyFile = new File(keyStorePath + "/public_key.pem");

            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                log.info("Generating new RSA key pair...");

                KeyPair keyPair = rsaKeyUtil.generateKeyPair();

                String privateKeyPem = rsaKeyUtil.encodePrivateKey(keyPair.getPrivate());
                String publicKeyPem = rsaKeyUtil.encodePublicKey(keyPair.getPublic());

                // Save private key
                try (FileWriter writer = new FileWriter(privateKeyFile)) {
                    writer.write("-----BEGIN PRIVATE KEY-----\n");
                    writer.write(privateKeyPem);
                    writer.write("\n-----END PRIVATE KEY-----\n");
                }

                // Save public key
                try (FileWriter writer = new FileWriter(publicKeyFile)) {
                    writer.write("-----BEGIN PUBLIC KEY-----\n");
                    writer.write(publicKeyPem);
                    writer.write("\n-----END PUBLIC KEY-----\n");
                }

                log.info("RSA key pair generated successfully at: {}", keyStorePath);
                log.info("Private key saved to: {}", privateKeyFile.getAbsolutePath());
                log.info("Public key saved to: {}", publicKeyFile.getAbsolutePath());
                log.info("Public key can be shared with other services for token verification");
            } else {
                log.info("RSA key pair already exists at: {}", keyStorePath);
            }
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public String getPublicKeyFromFile() throws IOException {
        String publicKeyPath = keyStorePath + "/public_key.pem";
        String content = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
        return content.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s", "");
    }

    public String getPrivateKeyFromFile() throws IOException {
        String privateKeyPath = keyStorePath + "/private_key.pem";
        String content = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
        return content.replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", "")
                      .replaceAll("\\s", "");
    }
}
