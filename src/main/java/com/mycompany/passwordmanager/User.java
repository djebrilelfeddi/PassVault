package com.mycompany.passwordmanager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import javax.crypto.SecretKey;

/**
 * Represents a user profile with encrypted password storage.
 * Handles key derivation, encryption/decryption, and password management.
 */
public class User {
    /**
     * Class containing decrypted credential details for UI use.
     */
    public static class PasswordEntry {
        private String username;
        private String password;
        private LocalDate expirationDate;
        /**
         * Creates a password entry with optional expiration date.
         *
         * @param username credential username
         * @param password decrypted password
         * @param expirationDate optional expiration date
         */
        public PasswordEntry(String username, String password, LocalDate expirationDate) {
            this.username = username;
            this.password = password;
            this.expirationDate = expirationDate;
        }
        
        /**
         * Returns the username.
         *
         * @return username
         */
        public String getUsername() {
            return username;
        }
        
        /**
         * Returns the password.
         *
         * @return password
         */
        public String getPassword() {
            return password;
        }
        
        /**
         * Returns the expiration date.
         *
         * @return expiration date
         */
        public LocalDate getExpirationDate() {
            return expirationDate;
        }
        
        /**
         * Sets the username.
         *
         * @param username new username
         */
        public void setUsername(String username) {
            this.username = username;
        }
        
        /**
         * Sets the password.
         *
         * @param password new password
         */
        public void setPassword(String password) {
            this.password = password;
        }
        
        /**
         * Sets the expiration date.
         *
         * @param expirationDate new expiration date
         */
        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }
    }
    
    private final String name;
    private final String masterPassword;
    private final String salt;
    private final Config config;
    private final Map<String, PasswordEntry> passwords;
    /**
     * Creates a new user profile with explicit cryptographic settings.
     *
     * @param name user identifier
     * @param masterPassword master password used for key derivation
     * @param algorithm symmetric algorithm
     * @param mode cipher mode
     * @throws Exception if key generation or persistence fails
     */
    User(String name, String masterPassword, String algorithm, String mode) 
            throws Exception {
        this.name = name;
        this.masterPassword = masterPassword;
        this.salt = generateSalt();
        this.passwords = new HashMap<>();
        
        SecretKey key = Encryption.getKeyFromPassword(masterPassword, algorithm, salt);
        byte[] iv = Encryption.generateIv();
        
        this.config = new Config(algorithm, mode, key, iv);
        
        FileManager.saveConfig(name, config, salt);
    }
    
    /**
     * Loads an existing user profile with stored cryptographic settings.
     *
     * @param name user identifier
     * @param masterPassword master password used for key derivation
     * @throws Exception if loading or validation fails
     */
    User(String name, String masterPassword) 
            throws Exception {
        this.name = name;
        this.masterPassword = masterPassword;
        this.passwords = new HashMap<>();
        
        FileManager.ConfigData configData = FileManager.loadConfig(name);
        if (configData == null) {
            throw new IOException("Utilisateur non trouvé : " + name);
        }
        
        this.salt = configData.getSalt();
        
        SecretKey key = Encryption.getKeyFromPassword(masterPassword, configData.getAlgorithm(), salt);
        this.config = new Config(configData.getAlgorithm(), configData.getMode(), key, configData.getIv());
        
        FileManager.validatePassword(
            name,
            masterPassword,
            configData.getAlgorithm(),
            configData.getMode(),
            salt,
            configData.getIv(),
            configData.getVerificationToken()
        );
        
        loadPasswords();
    }

    /**
     * Adds a new password entry.
     *
     * @param label label for the password
     * @param username associated username
     * @param password decrypted password
     * @param expirationDate optional expiration date
     * @throws Exception if encryption or saving fails
     */
    public void addPassword(String label, String username, String password, LocalDate expirationDate) throws Exception {
        String encrypted = Encryption.encrypt(
            password, 
            config.getAlgorithm(), 
            config.getMode(), 
            config.getKey(), 
            config.getIV()
        );
        
        FileManager.savePassword(name, label, username, encrypted, expirationDate);
        
        passwords.put(label, new PasswordEntry(username, password, expirationDate));
    }
    /**
     * Deletes a password entry by label.
     *
     * @param label label of the password to delete
     * @return {@code true} if deletion was successful, {@code false} if not found
     * @throws Exception if saving changes fails
     */
    public boolean deletePassword(String label) throws Exception {
        PasswordEntry removed = passwords.remove(label);
        if (removed == null) {
            return false;
        }

        FileManager.overwritePasswords(name, passwords, config);
        return true;
    }
    /**
     * Loads and decrypts all stored passwords for the user.
     *
     * @throws Exception if loading or decryption fails
     */
    private void loadPasswords() throws Exception {
        Map<String, String[]> encryptedPasswords = FileManager.loadPasswords(name);
        
        for (Map.Entry<String, String[]> entry : encryptedPasswords.entrySet()) {
            String label = entry.getKey();
            String[] data = entry.getValue();
            
            String username = data[0];
            String encrypted = data[1];
            String expirationDateStr = data.length > 2 ? data[2] : null;
            
            String decrypted = Encryption.decrypt(
                encrypted, 
                config.getAlgorithm(), 
                config.getMode(), 
                config.getKey(), 
                config.getIV()
            );
            
            LocalDate expirationDate = expirationDateStr != null && !expirationDateStr.isEmpty() 
                ? LocalDate.parse(expirationDateStr) 
                : null;
            
            passwords.put(label, new PasswordEntry(username, decrypted, expirationDate));
        }
    }
    
    /**
     * Retrieves a password entry by its label.
     *
     * @param label label of the password entry
     * @return the corresponding PasswordEntry or null if not found
     */
    public PasswordEntry getPasswordEntry(String label) {
        return passwords.get(label);
    }
    
    /**
     * Retrieves all password entries.
     *
     * @return a map of all password entries
     */
    public Map<String, PasswordEntry> getAllPasswords() {
        return new HashMap<>(passwords);
    }
    
    /**
     * Generates a new random salt for key derivation.
     *
     * @return base64-encoded salt string
     */
    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
    /**
     * Returns the user's configuration.
     *
     * @return user configuration
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * Returns the user's name.
     *
     * @return user name
     */
    public String getName() {
        return name;
    }
}
