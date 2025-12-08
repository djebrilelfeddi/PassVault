package com.mycompany.passwordmanager;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe FileManager.
 * Vérifie la persistance et le chargement des fichiers de configuration et mots de passe.
 */
@DisplayName("Tests de FileManager")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileManagerTest {

    private static final String TEST_USER = "filetest_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "TestPass123!";
    private static final String DATA_DIR = "user_data";
    private static Config testConfig;
    private static String testSalt;

    @BeforeAll
    static void setUpAll() throws Exception {
        testSalt = java.util.Base64.getEncoder().encodeToString("TestSalt1234567890".getBytes());
        SecretKey key = Encryption.getKeyFromPassword(TEST_PASSWORD, "AES", testSalt);
        byte[] iv = Encryption.generateIv();
        testConfig = new Config("AES", "GCM", key, iv);
    }

    @AfterEach
    void tearDown() throws IOException {
        File configFile = new File(DATA_DIR + File.separator + TEST_USER + "_config.txt");
        File passwordFile = new File(DATA_DIR + File.separator + TEST_USER + "_passwords.txt");
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(passwordFile.toPath());
    }

    @Test
    @Order(1)
    @DisplayName("Sauvegarde de la configuration utilisateur")
    void testSaveConfig() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        File configFile = new File(DATA_DIR + File.separator + TEST_USER + "_config.txt");
        assertTrue(configFile.exists(), "Le fichier de configuration doit exister");
        assertTrue(configFile.length() > 0, "Le fichier de configuration ne doit pas être vide");
    }

    @Test
    @Order(2)
    @DisplayName("Chargement de la configuration utilisateur")
    void testLoadConfig() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        FileManager.ConfigData loaded = FileManager.loadConfig(TEST_USER);
        assertNotNull(loaded, "La configuration chargée ne doit pas être null");
        assertEquals("AES", loaded.getAlgorithm(), "L'algorithme doit être AES");
        assertEquals("GCM", loaded.getMode(), "Le mode doit être GCM");
        assertEquals(testSalt, loaded.getSalt(), "Le salt doit correspondre");
        assertNotNull(loaded.getIv(), "L'IV ne doit pas être null");
    }

    @Test
    @Order(3)
    @DisplayName("Vérification qu'un utilisateur existe")
    void testUserExists() throws Exception {
        assertFalse(FileManager.userExists(TEST_USER), "L'utilisateur ne doit pas exister initialement");
        
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        assertTrue(FileManager.userExists(TEST_USER), "L'utilisateur doit exister après création");
    }

    @Test
    @Order(4)
    @DisplayName("Validation du mot de passe correct")
    void testValidateCorrectPassword() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        FileManager.ConfigData configData = FileManager.loadConfig(TEST_USER);
        
        assertDoesNotThrow(() -> {
            FileManager.validatePassword(
                TEST_USER,
                TEST_PASSWORD,
                configData.getAlgorithm(),
                configData.getMode(),
                testSalt,
                configData.getIv(),
                configData.getVerificationToken()
            );
        }, "La validation avec le bon mot de passe ne doit pas lever d'exception");
    }

    @Test
    @Order(5)
    @DisplayName("Validation du mot de passe incorrect échoue")
    void testValidateWrongPassword() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        FileManager.ConfigData configData = FileManager.loadConfig(TEST_USER);
        
        assertThrows(Exception.class, () -> {
            FileManager.validatePassword(
                TEST_USER,
                "WrongPassword",
                configData.getAlgorithm(),
                configData.getMode(),
                testSalt,
                configData.getIv(),
                configData.getVerificationToken()
            );
        }, "La validation avec un mauvais mot de passe doit échouer");
    }

    @Test
    @Order(6)
    @DisplayName("Sauvegarde d'un mot de passe")
    void testSavePassword() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        String encrypted = Encryption.encrypt("MyPassword", "AES", "GCM", testConfig.getKey(), testConfig.getIV());
        FileManager.savePassword(TEST_USER, "Gmail", "user@gmail.com", encrypted, LocalDate.now().plusDays(30));
        
        File passwordFile = new File(DATA_DIR + File.separator + TEST_USER + "_passwords.txt");
        assertTrue(passwordFile.exists(), "Le fichier de mots de passe doit exister");
        assertTrue(passwordFile.length() > 0, "Le fichier ne doit pas être vide");
    }

    @Test
    @Order(7)
    @DisplayName("Chargement des mots de passe")
    void testLoadPasswords() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        String encrypted1 = Encryption.encrypt("Pass1", "AES", "GCM", testConfig.getKey(), testConfig.getIV());
        String encrypted2 = Encryption.encrypt("Pass2", "AES", "GCM", testConfig.getKey(), testConfig.getIV());
        
        FileManager.savePassword(TEST_USER, "Gmail", "user1@gmail.com", encrypted1, null);
        FileManager.savePassword(TEST_USER, "Facebook", "user2@fb.com", encrypted2, LocalDate.now().plusDays(60));
        
        Map<String, String[]> passwords = FileManager.loadPasswords(TEST_USER);
        
        assertEquals(2, passwords.size(), "Il doit y avoir 2 mots de passe");
        assertTrue(passwords.containsKey("Gmail"), "Gmail doit exister");
        assertTrue(passwords.containsKey("Facebook"), "Facebook doit exister");
        
        String[] gmailData = passwords.get("Gmail");
        assertEquals("user1@gmail.com", gmailData[0], "Le username Gmail doit correspondre");
        
        String[] fbData = passwords.get("Facebook");
        assertEquals("user2@fb.com", fbData[0], "Le username Facebook doit correspondre");
        assertNotNull(fbData[2], "La date d'expiration ne doit pas être null");
    }

    @Test
    @Order(8)
    @DisplayName("Écrasement des mots de passe")
    void testOverwritePasswords() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        Map<String, User.PasswordEntry> entries = new HashMap<>();
        entries.put("Gmail", new User.PasswordEntry("user@gmail.com", "Pass1", null));
        entries.put("Twitter", new User.PasswordEntry("user@twitter.com", "Pass2", LocalDate.now().plusDays(45)));
        
        FileManager.overwritePasswords(TEST_USER, entries, testConfig);
        
        Map<String, String[]> loaded = FileManager.loadPasswords(TEST_USER);
        assertEquals(2, loaded.size(), "Il doit y avoir exactement 2 entrées");
        assertTrue(loaded.containsKey("Gmail"), "Gmail doit exister");
        assertTrue(loaded.containsKey("Twitter"), "Twitter doit exister");
    }

    @Test
    @Order(9)
    @DisplayName("Chargement d'un utilisateur inexistant retourne null")
    void testLoadNonExistentUser() throws Exception {
        FileManager.ConfigData config = FileManager.loadConfig("NonExistentUser_12345");
        assertNull(config, "La configuration d'un utilisateur inexistant doit être null");
    }

    @Test
    @Order(10)
    @DisplayName("Écrasement avec liste vide supprime le fichier")
    void testOverwriteWithEmptyMap() throws Exception {
        FileManager.saveConfig(TEST_USER, testConfig, testSalt);
        
        // Créer un fichier de mots de passe
        Map<String, User.PasswordEntry> entries = new HashMap<>();
        entries.put("Gmail", new User.PasswordEntry("user@gmail.com", "Pass1", null));
        FileManager.overwritePasswords(TEST_USER, entries, testConfig);
        
        File passwordFile = new File(DATA_DIR + File.separator + TEST_USER + "_passwords.txt");
        assertTrue(passwordFile.exists(), "Le fichier doit exister après ajout");
        
        // Écraser avec une map vide
        FileManager.overwritePasswords(TEST_USER, new HashMap<>(), testConfig);
        
        assertFalse(passwordFile.exists(), "Le fichier doit être supprimé après écrasement avec map vide");
    }
}
