package com.mycompany.passwordmanager;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration couvrant les scénarios utilisateur complets.
 */
@DisplayName("Tests d'intégration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static final String TEST_USER = "integration_" + System.currentTimeMillis();
    private static final String MASTER_PASSWORD = "IntegrationPass123!";
    private static final String DATA_DIR = "user_data";

    @AfterAll
    static void tearDownAll() throws IOException {
        File configFile = new File(DATA_DIR + File.separator + TEST_USER + "_config.txt");
        File passwordFile = new File(DATA_DIR + File.separator + TEST_USER + "_passwords.txt");
        Files.deleteIfExists(configFile.toPath());
        Files.deleteIfExists(passwordFile.toPath());
    }

    @Test
    @Order(1)
    @DisplayName("Scénario complet : création utilisateur, ajout et récupération de mots de passe")
    void testCompleteUserLifecycle() throws Exception {
        // 1. Créer un nouvel utilisateur
        User user1 = new User(TEST_USER, MASTER_PASSWORD, "AES", "GCM");
        assertNotNull(user1);
        
        // 2. Ajouter plusieurs mots de passe
        user1.addPassword("Gmail", "alice@gmail.com", "GmailPass123", LocalDate.now().plusDays(90));
        user1.addPassword("GitHub", "alice_dev", "GitHubPass456", null);
        user1.addPassword("AWS", "alice.aws@company.com", "AWSPass789", LocalDate.now().plusDays(180));
        
        // 3. Vérifier que les mots de passe sont en mémoire
        assertEquals(3, user1.getAllPasswords().size());
        
        // 4. Charger le même utilisateur depuis le disque
        User user2 = new User(TEST_USER, MASTER_PASSWORD);
        
        // 5. Vérifier que tous les mots de passe sont rechargés correctement
        Map<String, User.PasswordEntry> passwords = user2.getAllPasswords();
        assertEquals(3, passwords.size(), "Les 3 mots de passe doivent être rechargés");
        
        User.PasswordEntry gmailEntry = user2.getPasswordEntry("Gmail");
        assertEquals("alice@gmail.com", gmailEntry.getUsername());
        assertEquals("GmailPass123", gmailEntry.getPassword());
        assertNotNull(gmailEntry.getExpirationDate());
        
        User.PasswordEntry githubEntry = user2.getPasswordEntry("GitHub");
        assertEquals("alice_dev", githubEntry.getUsername());
        assertEquals("GitHubPass456", githubEntry.getPassword());
        assertNull(githubEntry.getExpirationDate());
        
        // 6. Supprimer un mot de passe
        assertTrue(user2.deletePassword("GitHub"));
        assertEquals(2, user2.getAllPasswords().size());
        
        // 7. Recharger à nouveau et vérifier la suppression
        User user3 = new User(TEST_USER, MASTER_PASSWORD);
        assertEquals(2, user3.getAllPasswords().size());
        assertNull(user3.getPasswordEntry("GitHub"));
        assertTrue(user3.getAllPasswords().containsKey("Gmail"));
        assertTrue(user3.getAllPasswords().containsKey("AWS"));
    }

    @Test
    @Order(2)
    @DisplayName("Scénario de sécurité : mauvais mot de passe après création")
    void testSecurityScenario() throws Exception {
        String uniqueUser = "security_" + System.currentTimeMillis();
        
        try {
            // Créer un utilisateur
            new User(uniqueUser, MASTER_PASSWORD, "AES", "CBC");
            
            // Tenter de se connecter avec un mauvais mot de passe
            assertThrows(Exception.class, () -> {
                new User(uniqueUser, "WrongPassword");
            });
            
            // Vérifier que la connexion avec le bon mot de passe fonctionne
            User validUser = new User(uniqueUser, MASTER_PASSWORD);
            assertNotNull(validUser);
            
        } finally {
            // Nettoyage
            File configFile = new File(DATA_DIR + File.separator + uniqueUser + "_config.txt");
            File passwordFile = new File(DATA_DIR + File.separator + uniqueUser + "_passwords.txt");
            Files.deleteIfExists(configFile.toPath());
            Files.deleteIfExists(passwordFile.toPath());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Scénario de persistance : multiples sessions utilisateur")
    void testMultipleSessionsPersistence() throws Exception {
        String sessionUser = "session_" + System.currentTimeMillis();
        
        try {
            //session 1 : Créer et ajouter des mots de passe
            User session1 = new User(sessionUser, MASTER_PASSWORD, "AES", "GCM");
            session1.addPassword("Service1", "user1", "pass1", null);
            session1.addPassword("Service2", "user2", "pass2", LocalDate.now().plusDays(30));
            
            //session 2 : Charger et modifier
            User session2 = new User(sessionUser, MASTER_PASSWORD);
            assertEquals(2, session2.getAllPasswords().size());
            session2.addPassword("Service3", "user3", "pass3", LocalDate.now().plusDays(60));
            session2.deletePassword("Service1");
            
            //session 3 : Vérifier les modifications
            User session3 = new User(sessionUser, MASTER_PASSWORD);
            Map<String, User.PasswordEntry> passwords = session3.getAllPasswords();
            assertEquals(2, passwords.size());
            assertNull(passwords.get("Service1"));
            assertNotNull(passwords.get("Service2"));
            assertNotNull(passwords.get("Service3"));
            
        } finally {
            File configFile = new File(DATA_DIR + File.separator + sessionUser + "_config.txt");
            File passwordFile = new File(DATA_DIR + File.separator + sessionUser + "_passwords.txt");
            Files.deleteIfExists(configFile.toPath());
            Files.deleteIfExists(passwordFile.toPath());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test de différents algorithmes de chiffrement")
    void testDifferentEncryptionAlgorithms() throws Exception {
        String[] algorithms = {"AES", "DES", "DESede"};
        String[] modes = {"CBC", "GCM"};
        
        for (String algo : algorithms) {
            for (String mode : modes) {
                // Ignorer les combinaisons non supportées
                if ("DES".equals(algo) && "GCM".equals(mode)) continue;
                if ("DESede".equals(algo) && "GCM".equals(mode)) continue;
                
                String testUser = "algo_" + algo + "_" + mode + "_" + System.currentTimeMillis();
                
                try {
                    User user = new User(testUser, MASTER_PASSWORD, algo, mode);
                    user.addPassword("TestService", "testuser", "testpass", null);
                    
                    // Recharger et vérifier
                    User reloaded = new User(testUser, MASTER_PASSWORD);
                    User.PasswordEntry entry = reloaded.getPasswordEntry("TestService");
                    assertEquals("testpass", entry.getPassword());
                    
                } finally {
                    File configFile = new File(DATA_DIR + File.separator + testUser + "_config.txt");
                    File passwordFile = new File(DATA_DIR + File.separator + testUser + "_passwords.txt");
                    Files.deleteIfExists(configFile.toPath());
                    Files.deleteIfExists(passwordFile.toPath());
                }
            }
        }
    }
}
