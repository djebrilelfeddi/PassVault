package com.mycompany.passwordmanager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Handles encrypted persistence of user configuration and password entries.
 * Responsible for saving/loading encrypted config and password files under "user_data".
 */
public class FileManager {
    private static final String DATA_DIR = "user_data";
    /**
     * Saves an encrypted user configuration file alongside a verification token.
     *
     * @param username account identifier
     * @param config encryption settings to persist
     * @param salt salt used for key derivation
     * @throws Exception if encryption or IO fails
     */    
    public static void saveConfig(String username, Config config, String salt) throws Exception {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        File userFile = new File(DATA_DIR + File.separator + username + "_config.txt");
        
        StringBuilder content = new StringBuilder();
        content.append("algorithm=").append(config.getAlgorithm()).append("\n");
        content.append("mode=").append(config.getMode()).append("\n");
        
        SecretKey configKey = Encryption.getKeyFromPassword(username + "_config_key", "AES", salt);
        
        String encryptedContent = Encryption.encrypt(
            content.toString(),
            "AES", "GCM", configKey, config.getIV()
        );
        
        String verificationToken = "VALID_PASSWORD";
        String encryptedToken = Encryption.encrypt(
            verificationToken,
            config.getAlgorithm(),
            config.getMode(),
            config.getKey(),
            config.getIV()
        );
        
        StringBuilder fileContent = new StringBuilder();
        fileContent.append(salt).append("||");
        fileContent.append(Base64.getEncoder().encodeToString(config.getIV())).append("||");
        fileContent.append("ENCRYPTED||");
        fileContent.append(encryptedContent);
        fileContent.append("||VERIFY||");
        fileContent.append(encryptedToken);
        
        Files.write(userFile.toPath(), fileContent.toString().getBytes(StandardCharsets.UTF_8));
    }
    /**
     * Reads and decrypts the configuration file for the requested user.
     *
     * @param username account identifier
     * @return decrypted configuration data or {@code null} if absent
     * @throws Exception if decryption or IO fails
     */    
    public static ConfigData loadConfig(String username) throws Exception {
        File userFile = new File(DATA_DIR + File.separator + username + "_config.txt");
        
        if (!userFile.exists()) {
            return null;
        }
        
        String fileContent = new String(Files.readAllBytes(userFile.toPath()), StandardCharsets.UTF_8);
        String[] fileParts = fileContent.split("\\|\\|ENCRYPTED\\|\\|", 2);
        
        if (fileParts.length < 2) {
            throw new IOException("Invalid config file format");
        }
        
        String[] headers = fileParts[0].split("\\|\\|");
        String salt = headers[0];
        byte[] iv = Base64.getDecoder().decode(headers[1]);
        
        String[] dataParts = fileParts[1].split("\\|\\|VERIFY\\|\\|", 2);
        String encryptedData = dataParts[0];
        String verificationToken = dataParts.length > 1 ? dataParts[1] : null;
        
        SecretKey configKey = Encryption.getKeyFromPassword(username + "_config_key", "AES", salt);
        
        String decryptedContent = Encryption.decrypt(
            encryptedData,
            "AES", "GCM", configKey, iv
        );
        
        String algorithm = "";
        String mode = "";
        
        String[] lines = decryptedContent.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                switch (parts[0]) {
                    case "algorithm":
                        algorithm = parts[1];
                        break;
                    case "mode":
                        mode = parts[1];
                        break;
                }
            }
        }
        
        return new ConfigData(algorithm, mode, salt, iv, verificationToken);
    }
    /**
     * Appends an encrypted password entry to the user credential store.
     *
     * @param username owner account
     * @param label human-readable credential label
     * @param usernameField stored username for the credential
     * @param encryptedPassword ciphertext produced by {@link Encryption}
     * @param expirationDate optional expiration date
     * @throws Exception if encryption or IO fails
     */    
    public static void savePassword(String username, String label, String usernameField, String encryptedPassword, LocalDate expirationDate) throws Exception {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        File passwordFile = new File(DATA_DIR + File.separator + username + "_passwords.txt");
        
        ConfigData configData = loadConfig(username);
        SecretKey fileKey = Encryption.getKeyFromPassword(username + "_file_encryption", "AES", configData.getSalt());
        
        StringBuilder content = new StringBuilder();
        
        if (passwordFile.exists()) {
            byte[] encryptedFileContent = Files.readAllBytes(passwordFile.toPath());
            if (encryptedFileContent.length > 0) {
                String decryptedContent = Encryption.decrypt(
                    Base64.getEncoder().encodeToString(encryptedFileContent),
                    "AES", "GCM", fileKey, configData.getIv()
                );
                content.append(decryptedContent);
            }
        }
        
        String expirationStr = expirationDate != null ? expirationDate.toString() : "";
        if (content.length() > 0) {
            content.append("\n");
        }
        content.append(label).append("||").append(usernameField).append("||").append(encryptedPassword).append("||").append(expirationStr);
        
        String encryptedContent = Encryption.encrypt(
            content.toString(),
            "AES", "GCM", fileKey, configData.getIv()
        );
        
        Files.write(passwordFile.toPath(), Base64.getDecoder().decode(encryptedContent));
    }
    /**
     * Loads and decrypts all stored password entries for the user.
     *
     * @param username account identifier
     * @return map of labels to username/password/expiration triplets
     * @throws Exception if decryption fails
     */    
    public static Map<String, String[]> loadPasswords(String username) throws Exception {
        File passwordFile = new File(DATA_DIR + File.separator + username + "_passwords.txt");
        Map<String, String[]> passwords = new HashMap<>();
        
        if (!passwordFile.exists()) {
            return passwords;
        }
        
        ConfigData configData = loadConfig(username);
        SecretKey fileKey = Encryption.getKeyFromPassword(username + "_file_encryption", "AES", configData.getSalt());
        
        byte[] encryptedFileContent = Files.readAllBytes(passwordFile.toPath());
        if (encryptedFileContent.length == 0) {
            return passwords;
        }
        
        String decryptedContent = Encryption.decrypt(
            Base64.getEncoder().encodeToString(encryptedFileContent),
            "AES", "GCM", fileKey, configData.getIv()
        );
        
        String[] lines = decryptedContent.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split("\\|\\|");
            if (parts.length >= 3) {
                String label = parts[0];
                String usernameField = parts[1];
                String encrypted = parts[2];
                String expirationDate = parts.length > 3 ? parts[3] : "";
                passwords.put(label, new String[]{usernameField, encrypted, expirationDate});
            }
        }
        
        return passwords;
    }
    /**
     * Overwrites the password file with the provided entries (after deletion/update).
     *
     * @param username account identifier
     * @param entries in-memory credential snapshot
     * @param config encryption configuration for the session
     * @throws Exception if encryption or IO fails
     */
    public static void overwritePasswords(String username, Map<String, User.PasswordEntry> entries, Config config) throws Exception {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File passwordFile = new File(DATA_DIR + File.separator + username + "_passwords.txt");

        if (entries.isEmpty()) {
            Files.deleteIfExists(passwordFile.toPath());
            return;
        }

        ConfigData configData = loadConfig(username);
        if (configData == null) {
            throw new IOException("Configuration introuvable pour l'utilisateur : " + username);
        }

        SecretKey fileKey = Encryption.getKeyFromPassword(username + "_file_encryption", "AES", configData.getSalt());

        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, User.PasswordEntry> mapEntry : entries.entrySet()) {
            User.PasswordEntry entry = mapEntry.getValue();
            String usernameField = entry.getUsername() != null ? entry.getUsername() : "";
            String encryptedPassword = Encryption.encrypt(
                entry.getPassword(),
                config.getAlgorithm(),
                config.getMode(),
                config.getKey(),
                config.getIV()
            );
            String expirationStr = entry.getExpirationDate() != null ? entry.getExpirationDate().toString() : "";

            if (content.length() > 0) {
                content.append("\n");
            }

            content.append(mapEntry.getKey())
                   .append("||")
                   .append(usernameField)
                   .append("||")
                   .append(encryptedPassword)
                   .append("||")
                   .append(expirationStr);
        }

        String encryptedContent = Encryption.encrypt(
            content.toString(),
            "AES", "GCM", fileKey, configData.getIv()
        );

        Files.write(passwordFile.toPath(), Base64.getDecoder().decode(encryptedContent));
    }
    /**
     * Validates a master password by decrypting the stored verification token.
     *
     * @param username account identifier
     * @param masterPassword candidate master password
     * @param algorithm cipher algorithm
     * @param mode cipher mode
     * @param salt key-derivation salt
     * @param iv initialization vector
     * @param verificationToken encrypted verification marker
     * @throws Exception if validation fails or password is incorrect
     */    
    public static void validatePassword(String username, String masterPassword, String algorithm, String mode, String salt, byte[] iv, String verificationToken) throws Exception {
        if (verificationToken == null || verificationToken.isEmpty()) {
            throw new IOException("No verification token found");
        }
        
        SecretKey key = Encryption.getKeyFromPassword(masterPassword, algorithm, salt);
        
        String decrypted = Encryption.decrypt(verificationToken, algorithm, mode, key, iv);
        
        if (!"VALID_PASSWORD".equals(decrypted)) {
            throw new javax.crypto.BadPaddingException("Invalid password");
        }
    }
    /**
     * @param username account identifier
     * @return {@code true} if a configuration file exists
     */    
    public static boolean userExists(String username) {
        File userFile = new File(DATA_DIR + File.separator + username + "_config.txt");
        return userFile.exists();
    }
    
    /**
     * Container for decrypted configuration data loaded from file.
     * Used internally to pass multiple config fields.
     */
    public static class ConfigData {
        private final String algorithm;
        private final String mode;
        private final String salt;
        private final byte[] iv;
        private final String verificationToken;
        /**
         * Creates a new configuration data container.
         *
         * @param algorithm cipher algorithm
         * @param mode cipher mode
         * @param salt key-derivation salt
         * @param iv initialization vector
         * @param verificationToken encrypted verification string
         */
        public ConfigData(String algorithm, String mode, String salt, byte[] iv, String verificationToken) {
            this.algorithm = algorithm;
            this.mode = mode;
            this.salt = salt;
            this.iv = iv != null ? iv.clone() : null;
            this.verificationToken = verificationToken;
        }
        /**
         * @return cipher algorithm stored for the user
         */
        public String getAlgorithm() {
            return algorithm;
        }
        /**
         * @return cipher mode stored for the user
         */
        public String getMode() {
            return mode;
        }
        /**
         * @return salt used for key derivation
         */
        public String getSalt() {
            return salt;
        }
        /**
         * @return defensive copy of the initialization vector
         */
        public byte[] getIv() {
            return iv != null ? iv.clone() : null;
        }
        /**
         * @return encrypted verification token
         */
        public String getVerificationToken() {
            return verificationToken;
        }
    }
}
