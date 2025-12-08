package com.mycompany.passwordmanager;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe User.
 * Vérifie la gestion des utilisateurs et des mots de passe.
 */
@DisplayName("Tests de la classe User")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserTest {

    private static final String TEST_USER = "testuser_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "MasterPass123!";
    private static final String DATA_DIR = "user_data";

    @BeforeEach
    void setUp() throws IOException {
        // Nettoyer les fichiers de test existants
        cleanupTestFiles();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Nettoyer après chaque test
        cleanupTestFiles();
    }

    private void cleanupTestFiles() throws IOException {
        File configFile = new File(DATA_DIR + File.separator + TEST_USER + "_config.txt");
        File passwordFile = new File(DATA_DIR + File.separator + TEST_USER + "_passwords.txt");
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(passwordFile.toPath());
    }

    @Test
    @Order(1)
    @DisplayName("Création d'un nouvel utilisateur")
    void testCreateNewUser() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "CBC");
        assertNotNull(user, "L'utilisateur créé ne doit pas être null");
        assertEquals(TEST_USER, user.getName(), "Le nom d'utilisateur doit correspondre");
        assertNotNull(user.getConfig(), "La configuration ne doit pas être null");
    }

    @Test
    @Order(2)
    @DisplayName("Ajout d'un mot de passe")
    void testAddPassword() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "GCM");
        
        user.addPassword("Gmail", "user@gmail.com", "SecretPass123", null);
        
        Map<String, User.PasswordEntry> passwords = user.getAllPasswords();
        assertEquals(1, passwords.size(), "Il doit y avoir 1 mot de passe");
        assertTrue(passwords.containsKey("Gmail"), "Le label 'Gmail' doit exister");
        
        User.PasswordEntry entry = user.getPasswordEntry("Gmail");
        assertEquals("user@gmail.com", entry.getUsername(), "Le username doit correspondre");
        assertEquals("SecretPass123", entry.getPassword(), "Le password doit correspondre");
    }

    @Test
    @Order(3)
    @DisplayName("Ajout de plusieurs mots de passe")
    void testAddMultiplePasswords() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "CBC");
        
        user.addPassword("Gmail", "user@gmail.com", "Pass1", null);
        user.addPassword("Facebook", "user@fb.com", "Pass2", LocalDate.now().plusDays(30));
        user.addPassword("Twitter", "user@twitter.com", "Pass3", LocalDate.now().plusDays(60));
        
        Map<String, User.PasswordEntry> passwords = user.getAllPasswords();
        assertEquals(3, passwords.size(), "Il doit y avoir 3 mots de passe");
    }

    @Test
    @Order(4)
    @DisplayName("Suppression d'un mot de passe")
    void testDeletePassword() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "GCM");
        
        user.addPassword("Gmail", "user@gmail.com", "Pass1", null);
        user.addPassword("Facebook", "user@fb.com", "Pass2", null);
        
        boolean deleted = user.deletePassword("Gmail");
        assertTrue(deleted, "La suppression doit réussir");
        
        Map<String, User.PasswordEntry> passwords = user.getAllPasswords();
        assertEquals(1, passwords.size(), "Il ne doit rester qu'1 mot de passe");
        assertFalse(passwords.containsKey("Gmail"), "Gmail ne doit plus exister");
        assertTrue(passwords.containsKey("Facebook"), "Facebook doit toujours exister");
    }

    @Test
    @Order(5)
    @DisplayName("Suppression d'un mot de passe inexistant")
    void testDeleteNonExistentPassword() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "CBC");
        
        boolean deleted = user.deletePassword("NonExistent");
        assertFalse(deleted, "La suppression d'un mot de passe inexistant doit retourner false");
    }

    @Test
    @Order(6)
    @DisplayName("Persistance et rechargement des mots de passe")
    void testPasswordPersistence() throws Exception {
        // Créer un utilisateur et ajouter des mots de passe
        User user1 = new User(TEST_USER, TEST_PASSWORD, "AES", "GCM");
        user1.addPassword("Gmail", "user@gmail.com", "Pass1", LocalDate.now().plusDays(30));
        user1.addPassword("Facebook", "user@fb.com", "Pass2", null);
        
        // Charger le même utilisateur depuis les fichiers
        User user2 = new User(TEST_USER, TEST_PASSWORD);
        
        Map<String, User.PasswordEntry> passwords = user2.getAllPasswords();
        assertEquals(2, passwords.size(), "Les 2 mots de passe doivent être rechargés");
        
        User.PasswordEntry gmailEntry = user2.getPasswordEntry("Gmail");
        assertEquals("user@gmail.com", gmailEntry.getUsername());
        assertEquals("Pass1", gmailEntry.getPassword());
        assertNotNull(gmailEntry.getExpirationDate());
        
        User.PasswordEntry fbEntry = user2.getPasswordEntry("Facebook");
        assertEquals("user@fb.com", fbEntry.getUsername());
        assertEquals("Pass2", fbEntry.getPassword());
        assertNull(fbEntry.getExpirationDate());
    }

    @Test
    @Order(7)
    @DisplayName("Chargement avec mauvais mot de passe échoue")
    void testLoadWithWrongPassword() throws Exception {
        // Créer un utilisateur
        new User(TEST_USER, TEST_PASSWORD, "AES", "CBC");
        
        // Tenter de charger avec un mauvais mot de passe
        assertThrows(Exception.class, () -> {
            new User(TEST_USER, "WrongPassword");
        }, "Le chargement avec un mauvais mot de passe doit échouer");
    }

    @Test
    @Order(8)
    @DisplayName("Gestion des dates d'expiration")
    void testExpirationDates() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "GCM");
        
        LocalDate future = LocalDate.now().plusDays(90);
        user.addPassword("ExpiresSoon", "user@test.com", "Pass1", future);
        
        User.PasswordEntry entry = user.getPasswordEntry("ExpiresSoon");
        assertEquals(future, entry.getExpirationDate(), "La date d'expiration doit correspondre");
    }

    @Test
    @Order(9)
    @DisplayName("Modification d'un mot de passe (suppression + ajout)")
    void testModifyPassword() throws Exception {
        User user = new User(TEST_USER, TEST_PASSWORD, "AES", "CBC");
        
        user.addPassword("Gmail", "user@gmail.com", "OldPass", null);
        user.deletePassword("Gmail");
        user.addPassword("Gmail", "user@gmail.com", "NewPass", LocalDate.now().plusDays(60));
        
        User.PasswordEntry entry = user.getPasswordEntry("Gmail");
        assertEquals("NewPass", entry.getPassword(), "Le nouveau mot de passe doit être utilisé");
        assertNotNull(entry.getExpirationDate(), "La nouvelle date d'expiration doit exister");
    }
}
