package com.mycompany.passwordmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import javax.crypto.SecretKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe Encryption.
 * Vérifie le chiffrement/déchiffrement avec différents algorithmes et modes.
 */
@DisplayName("Tests de chiffrement/déchiffrement")
class EncryptionTest {

    private static final String TEST_PASSWORD = "TestMasterPassword123!";
    private static final String TEST_SALT = Base64.getEncoder().encodeToString("TestSalt1234567890".getBytes());
    private static final String PLAIN_TEXT = "MySecretPassword";

    @Test
    @DisplayName("Génération d'IV produit un tableau de 16 octets")
    void testGenerateIv() {
        byte[] iv = Encryption.generateIv();
        assertNotNull(iv, "L'IV ne doit pas être null");
        assertEquals(16, iv.length, "L'IV doit faire 16 octets");
    }

    @Test
    @DisplayName("Dérivation de clé AES-256 depuis un mot de passe")
    void testGetKeyFromPasswordAES() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", TEST_SALT);
        assertNotNull(key, "La clé ne doit pas être null");
        assertEquals("AES", key.getAlgorithm(), "L'algorithme doit être AES");
        assertEquals(32, key.getEncoded().length, "La clé AES-256 doit faire 32 octets");
    }

    @Test
    @DisplayName("Dérivation de clé DES depuis un mot de passe")
    void testGetKeyFromPasswordDES() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "DES", TEST_SALT);
        assertNotNull(key, "La clé ne doit pas être null");
        assertEquals("DES", key.getAlgorithm(), "L'algorithme doit être DES");
        assertEquals(8, key.getEncoded().length, "La clé DES doit faire 8 octets");
    }

    @Test
    @DisplayName("Chiffrement/déchiffrement AES-CBC réussi")
    void testEncryptDecryptAES_CBC() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", TEST_SALT);
        byte[] iv = Encryption.generateIv();

        String encrypted = Encryption.encrypt(PLAIN_TEXT, "AES", "CBC", key, iv);
        assertNotNull(encrypted, "Le texte chiffré ne doit pas être null");
        assertNotEquals(PLAIN_TEXT, encrypted, "Le texte chiffré doit différer du texte clair");

        String decrypted = Encryption.decrypt(encrypted, "AES", "CBC", key, iv);
        assertEquals(PLAIN_TEXT, decrypted, "Le déchiffrement doit restituer le texte original");
    }

    @Test
    @DisplayName("Chiffrement/déchiffrement AES-GCM réussi")
    void testEncryptDecryptAES_GCM() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", TEST_SALT);
        byte[] iv = Encryption.generateIv();

        String encrypted = Encryption.encrypt(PLAIN_TEXT, "AES", "GCM", key, iv);
        assertNotNull(encrypted, "Le texte chiffré ne doit pas être null");

        String decrypted = Encryption.decrypt(encrypted, "AES", "GCM", key, iv);
        assertEquals(PLAIN_TEXT, decrypted, "Le déchiffrement doit restituer le texte original");
    }

    @Test
    @DisplayName("Chiffrement/déchiffrement DES-CBC réussi")
    void testEncryptDecryptDES_CBC() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "DES", TEST_SALT);
        byte[] iv = Encryption.generateIv();

        String encrypted = Encryption.encrypt(PLAIN_TEXT, "DES", "CBC", key, iv);
        String decrypted = Encryption.decrypt(encrypted, "DES", "CBC", key, iv);
        assertEquals(PLAIN_TEXT, decrypted, "Le déchiffrement DES doit fonctionner");
    }

    @Test
    @DisplayName("Déchiffrement avec mauvaise clé échoue")
    void testDecryptWithWrongKey() throws Exception {
        SecretKey correctKey = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", TEST_SALT);
        SecretKey wrongKey = Encryption.getKeyFromPassword("WrongPassword", "AES", TEST_SALT);
        byte[] iv = Encryption.generateIv();

        String encrypted = Encryption.encrypt(PLAIN_TEXT, "AES", "CBC", correctKey, iv);

        assertThrows(Exception.class, () -> {
            Encryption.decrypt(encrypted, "AES", "CBC", wrongKey, iv);
        }, "Le déchiffrement avec une mauvaise clé doit échouer");
    }

    @Test
    @DisplayName("Déchiffrement avec mauvais IV échoue")
    void testDecryptWithWrongIV() throws Exception {
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", TEST_SALT);
        byte[] correctIv = Encryption.generateIv();
        byte[] wrongIv = Encryption.generateIv();

        String encrypted = Encryption.encrypt(PLAIN_TEXT, "AES", "CBC", key, correctIv);

        assertThrows(Exception.class, () -> {
            Encryption.decrypt(encrypted, "AES", "CBC", key, wrongIv);
        }, "Le déchiffrement avec un mauvais IV doit échouer");
    }

    @Test
    @DisplayName("Algorithme non supporté lève une exception")
    void testUnsupportedAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> {
            Encryption.getKeyFromPassword(TEST_PASSWORD, "INVALID_ALGO", TEST_SALT);
        }, "Un algorithme invalide doit lever IllegalArgumentException");
    }
}
