package com.mycompany.passwordmanager;

import javax.crypto.SecretKey;

/**
 * Container for encryption settings used during a user session.
 * Holds algorithm, cipher mode, derived key and initialization vector (IV).
 */
public final class Config {
    private final String algorithm;
    private final String mode;
    private final SecretKey key;
    private final byte[] iv;
    /**
     * Creates a new configuration snapshot.
     *
     * @param algorithm cipher algorithm (e.g. AES)
     * @param mode cipher mode (e.g. CBC, GCM)
     * @param key derived encryption key
     * @param iv initialization vector associated with the configuration
     */
    Config(String algorithm, String mode, SecretKey key, byte[] iv) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.key = key;
        this.iv = iv != null ? iv.clone() : null;
    }
    /**
     * @return configured cipher algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }
    /**
     * @return configured cipher mode
     */
    public String getMode() {
        return mode;
    }
    /**
     * @return encryption key used for credential operations
     */
    public SecretKey getKey() {
        return key;
    }
    /**
     * @return defensive copy of the initialization vector, or {@code null}
     */
    public byte[] getIV() {
        return iv != null ? iv.clone() : null;
    }
}
