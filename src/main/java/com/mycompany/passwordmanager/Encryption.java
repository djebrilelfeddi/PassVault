package com.mycompany.passwordmanager;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class providing key derivation and encryption/decryption helpers.
 * Use static methods to derive keys (PBKDF2) and to encrypt/decrypt strings.
 */
public final class Encryption {
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 65_536;
    private static final int GCM_TAG_LENGTH = 128;

    private Encryption() {}
        /**
     * Derives a {@link SecretKey} from a password using PBKDF2 with HmacSHA256.
     *
     * @param password clear-text password
     * @param algorithm cipher algorithm that determines key size
     * @param salt textual salt used during key derivation
     * @return derived secret key
     * @throws NoSuchAlgorithmException if PBKDF2 is unavailable
     * @throws InvalidKeySpecException if key derivation fails
     */
    public static SecretKey getKeyFromPassword(String password, String algorithm, String salt)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

        int keySize;
        switch (algorithm) {
            case "AES":
                keySize = 256;
                break;
            case "DES":
                keySize = 56;
                break;
            case "DESede":
                keySize = 168;
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), DEFAULT_ITERATIONS, keySize);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), algorithm);
    }
    /**
     * Generates a random 16-byte initialization vector.
     *
     * @return new initialization vector
     */
    public static byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    /**
     * Encrypts a UTF-8 string with the supplied algorithm/mode/key/IV combination.
     *
     * @param input plain-text data
     * @param algorithm symmetric algorithm name
     * @param mode cipher mode (CBC, ECB, GCM)
     * @param key encryption key
     * @param iv initialization vector (used for CBC/GCM)
     * @return Base64 encoded ciphertext
     * @throws NoSuchPaddingException if padding is unsupported
     * @throws NoSuchAlgorithmException if the transformation is unavailable
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     * @throws InvalidKeyException if the key is incompatible
     * @throws BadPaddingException if padding fails during encryption
     * @throws IllegalBlockSizeException if the input size is invalid
     */
    public static String encrypt(String input, String algorithm, String mode, SecretKey key, byte[] iv) 
        throws NoSuchPaddingException, NoSuchAlgorithmException,
               InvalidAlgorithmParameterException, InvalidKeyException,
               BadPaddingException, IllegalBlockSizeException {

        String transformation = "GCM".equals(mode)
                ? algorithm + "/GCM/NoPadding"
                : algorithm + "/" + mode + "/PKCS5Padding";

        Cipher cipher = Cipher.getInstance(transformation);
        
        if ("GCM".equals(mode)) {
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        } else if ("CBC".equals(mode)) {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }
        
        byte[] cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }
    /**
     * Decrypts a Base64 encoded ciphertext back into UTF-8 text.
     *
     * @param input Base64 ciphertext
     * @param algorithm symmetric algorithm name
     * @param mode cipher mode (CBC, ECB, GCM)
     * @param key decryption key
     * @param iv initialization vector (used for CBC/GCM)
     * @return decrypted plain-text
     * @throws NoSuchPaddingException if padding is unsupported
     * @throws NoSuchAlgorithmException if the transformation is unavailable
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     * @throws InvalidKeyException if the key is incompatible
     * @throws BadPaddingException if authentication/tag validation fails
     * @throws IllegalBlockSizeException if the ciphertext length is invalid
     */
    public static String decrypt(String input, String algorithm, String mode, SecretKey key, byte[] iv) 
        throws NoSuchPaddingException, NoSuchAlgorithmException, 
               InvalidAlgorithmParameterException, InvalidKeyException,
               BadPaddingException, IllegalBlockSizeException {

        String transformation = "GCM".equals(mode)
                ? algorithm + "/GCM/NoPadding"
                : algorithm + "/" + mode + "/PKCS5Padding";

        Cipher cipher = Cipher.getInstance(transformation);
        
        if ("GCM".equals(mode)) {
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        } else if ("CBC".equals(mode)) {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key);
        }
        
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(input));
        return new String(plainText, StandardCharsets.UTF_8);
    }
    }
