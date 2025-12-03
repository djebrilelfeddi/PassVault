# 🔐 PassVault - Gestionnaire de Mots de Passe Sécurisé

[![Java](https://img.shields.io/badge/Java-11+-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-13-blue?style=for-the-badge&logo=java)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-Build-red?style=for-the-badge&logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
[![Security](https://img.shields.io/badge/Encryption-AES--256--GCM-purple?style=for-the-badge&logo=shield)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)

**PassVault** est un gestionnaire de mots de passe **local et sécurisé** développé en Java avec une interface graphique moderne JavaFX. Il offre un chiffrement de niveau militaire avec **AES-256-GCM** pour protéger vos identifiants sensibles.

> 🛡️ **Vos données restent sur votre machine** - Aucune connexion internet requise, aucun cloud, confidentialité totale.

---

## ✨ Fonctionnalités

### 🔒 Sécurité Avancée
- **Chiffrement AES-256-GCM** - Standard de chiffrement utilisé par les gouvernements et institutions financières
- **Double couche de chiffrement** - Fichiers chiffrés + mots de passe individuels encodés
- **Dérivation de clé PBKDF2** avec HmacSHA256 (65,536 itérations)
- **Support multi-algorithmes** : AES, DES, Triple DES (DESede)
- **Modes de chiffrement** : CBC, ECB, GCM

### 🎯 Gestion Intelligente
- **Stockage illimité** de mots de passe
- **Recherche instantanée** dans votre coffre-fort
- **Dates d'expiration** configurables avec alertes
- **Dashboard statistique** - Vue d'ensemble de vos identifiants
- **Catégorisation** par labels et usernames

### 🎨 Interface Utilisateur Moderne
- **Design épuré** avec JavaFX et CSS personnalisé
- **Animations fluides** sur l'écran de connexion
- **Interface responsive** et intuitive
- **Confirmations de sécurité** avant actions critiques

---

## 🚀 Installation

### Prérequis
- **Java 11** ou supérieur ([Télécharger OpenJDK](https://adoptium.net/))
- **Maven 3.6+** ([Télécharger Maven](https://maven.apache.org/download.cgi))

### Build & Exécution

```bash
# Cloner le repository
git clone https://github.com/votre-username/PassVault.git
cd PassVault

# Compiler et exécuter
mvn clean javafx:run

# OU créer un exécutable autonome
mvn clean package javafx:jlink
```

L'exécutable sera généré dans : `target/PasswordManager-1.0-SNAPSHOT/bin/PasswordManager`

### Alternative avec NetBeans
Si vous rencontrez des problèmes, vous pouvez ouvrir le projet dans **NetBeans** et utiliser le build intégré.

---

## 📖 Guide d'Utilisation

### 🆕 Première Utilisation

1. **Lancez PassVault**
2. **Créez un compte** avec un nom d'utilisateur unique
3. **Choisissez votre algorithme** de chiffrement (AES recommandé)
4. **Sélectionnez le mode** de chiffrement (GCM recommandé pour la sécurité maximale)
5. **Définissez un mot de passe maître fort** - C'est la clé de votre coffre-fort !

### 💾 Ajouter un Mot de Passe

1. Remplissez le **label** (ex: "Gmail", "Netflix")
2. Entrez le **nom d'utilisateur** associé
3. Saisissez le **mot de passe** à stocker
4. *(Optionnel)* Définissez une **date d'expiration**
5. Cliquez sur **Ajouter**

### 🔍 Rechercher & Consulter

- Utilisez la **barre de recherche** pour filtrer vos entrées
- **Sélectionnez** un élément et cliquez sur "Afficher" pour voir les détails
- Les mots de passe sont **décryptés à la volée** uniquement lors de l'affichage

---

## 🏗️ Architecture Technique

```
PassVault/
├── src/main/java/com/mycompany/passwordmanager/
│   ├── App.java              # Point d'entrée
│   ├── MainClass.java        # Initialisation JavaFX
│   ├── LoginController.java  # Authentification & inscription
│   ├── PrimaryController.java# Interface principale du coffre
│   ├── User.java             # Modèle utilisateur & gestion MDP
│   ├── Encryption.java       # Utilitaires cryptographiques
│   ├── Config.java           # Configuration de session
│   └── FileManager.java      # Persistance chiffrée
├── src/main/resources/
│   ├── login.fxml            # Vue de connexion
│   ├── primary.fxml          # Vue principale
│   └── styles.css            # Styles personnalisés
└── pom.xml                   # Configuration Maven
```

### 🔐 Flux de Chiffrement

```
Mot de passe → PBKDF2 (65,536 itérations) → Clé AES-256
                         ↓
Données sensibles → AES-256-GCM → Fichier chiffré (.txt)
                         ↓
         Chaque MDP → Encodage utilisateur (Algo+Mode choisi)
```

---

## 📊 Stockage des Données

Les fichiers utilisateur sont stockés dans `target/PasswordManager-1.0-SNAPSHOT/bin/users_data/` :

| Fichier | Description |
|---------|-------------|
| `<username>_config.txt` | Configuration chiffrée (algo, mode, sel, IV) |
| `<username>_password.txt` | Coffre-fort de mots de passe chiffré |

---

## 🛡️ Bonnes Pratiques de Sécurité

1. **Mot de passe maître fort** - Minimum 12 caractères, mélangez majuscules, minuscules, chiffres et symboles
2. **Ne partagez jamais** votre mot de passe maître
3. **Sauvegardez** régulièrement le dossier `users_data`
4. **Utilisez GCM** comme mode de chiffrement (authentification intégrée)
5. **Définissez des expirations** pour forcer le renouvellement des mots de passe

---

## 📚 Documentation API

La documentation Javadoc complète est générée automatiquement :

```bash
mvn javadoc:javadoc
```

Consultez-la dans : `target/reports/apidocs/index.html`

---

## 🔧 Technologies Utilisées

| Technologie | Version | Usage |
|-------------|---------|-------|
| **Java** | 11+ | Langage principal |
| **JavaFX** | 13 | Interface graphique |
| **Maven** | 3.6+ | Gestion de build |
| **PBKDF2** | - | Dérivation de clé |
| **AES-256-GCM** | - | Chiffrement principal |
| **SceneBuilder** | - | Design FXML |

---

## 🤝 Contribution

Les contributions sont les bienvenues ! 

1. **Fork** le projet
2. Créez une **branche** (`git checkout -b feature/AmazingFeature`)
3. **Committez** vos changements (`git commit -m 'Add AmazingFeature'`)
4. **Push** sur la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une **Pull Request**

---

## 📜 Licence

Distribué sous licence MIT. Voir `LICENSE` pour plus d'informations.

---

## 📞 Support

- 📧 **Email** : [votre-email@example.com]
- 🐛 **Issues** : [GitHub Issues](https://github.com/votre-username/PassVault/issues)

---

## 🌟 Remerciements

- [OpenJFX](https://openjfx.io/) pour JavaFX
- [Apache Maven](https://maven.apache.org/) pour le système de build
- La communauté Java pour les bonnes pratiques de sécurité

---

<p align="center">
  <b>⭐ Si PassVault vous est utile, n'hésitez pas à mettre une étoile au projet ! ⭐</b>
</p>

---

### 🔑 Mots-clés SEO

`password manager` `gestionnaire mot de passe` `java password manager` `javafx application` `secure password storage` `aes-256 encryption` `local password manager` `offline password manager` `open source password manager` `cryptographie java` `pbkdf2 java` `chiffrement aes` `coffre-fort numérique` `sécurité informatique` `gestion identifiants`
