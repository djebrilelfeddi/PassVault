package com.mycompany.passwordmanager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Controller for the login and registration screen.
 * Manages authentication, registration, and the animated login background.
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField masterPasswordField;
    @FXML private ComboBox<String> algorithmCombo;
    @FXML private ComboBox<String> modeCombo;
    @FXML private Label statusLabel;
    @FXML private VBox newUserOptions;
    @FXML private Pane backgroundPane;
    @FXML private Circle circle1;
    @FXML private Circle circle2;
    @FXML private Circle circle3;
    
    private static User currentUser;
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    /**
     * Configures default UI state and animated background.
    */
    @FXML
    private void initialize() {
        newUserOptions.setVisible(false);
        newUserOptions.setManaged(false);
        
        algorithmCombo.setValue("AES");
        modeCombo.setValue("CBC");
        
        initializeAnimatedBackground();
        
        newUserOptions.managedProperty().addListener((obs, wasManaged, isManaged) -> {
            Platform.runLater(() -> {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.sizeToScene();
            });
        });
    }
    /**
     * Starts continuous animations for the decorative circles.
     */    
    private void initializeAnimatedBackground() {
        animateCircle(circle1, 100, 80, 8000);
        animateCircle(circle2, 300, 200, 10000);
        animateCircle(circle3, 150, 300, 12000);
    }
    /**
     * Animates a circle along a pseudo-elliptic path.
     *
     * @param circle circle node to animate
     * @param startX starting X coordinate
     * @param startY starting Y coordinate
     * @param duration animation duration in milliseconds
     */    
    private void animateCircle(Circle circle, double startX, double startY, long duration) {
        circle.setCenterX(startX);
        circle.setCenterY(startY);
        
        AnimationTimer timer = new AnimationTimer() {
            private long startTime = -1;
            
            @Override
            public void handle(long now) {
                if (startTime < 0) startTime = now;
                long elapsedTime = now - startTime;
                double t = (elapsedTime % (duration * 1_000_000.0)) / (duration * 1_000_000.0);
                
                double angle = t * 2 * Math.PI;
                double offsetX = Math.cos(angle) * 80;
                double offsetY = Math.sin(angle * 1.5) * 60;
                
                circle.setCenterX(startX + offsetX);
                circle.setCenterY(startY + offsetY);
            }
        };
        timer.start();
    }
    /**
     * Attempts to authenticate the user and open the primary view.
     */   
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = masterPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields");
            return;
        }
        
        try {
            if (!FileManager.userExists(username)) {
                statusLabel.setText("User not found. Please register.");
                newUserOptions.setVisible(true);
                newUserOptions.setManaged(true);
                return;
            }
            
            currentUser = new User(username, password);
            statusLabel.setText("Login successful!");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            loginAttempts = 0;
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            MainCLass.setRoot("primary");
            Platform.runLater(() -> {
                stage.centerOnScreen();
            });
            
        } catch (Exception e) {
            loginAttempts++;
            int remainingAttempts = MAX_LOGIN_ATTEMPTS - loginAttempts;
            
            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                statusLabel.setText("Too many failed attempts. Application will close.");
                statusLabel.setStyle("-fx-text-fill: red;");
                masterPasswordField.disableProperty();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Too many mistakes..");
                alert.setHeaderText("Looks like you tried a bit too much here.");
                alert.setContentText("Application is closing...");
                alert.showAndWait();
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            } else {
                statusLabel.setText("Incorrect password! " + remainingAttempts + " attempt(s) remaining.");
                statusLabel.setStyle("-fx-text-fill: orange;");
            }
        }
    }
    
    /**
     * Handles new user registration and initializes encryption settings.
     */
    @FXML
    private void handleRegister() {
        if (!newUserOptions.isVisible()) {
            newUserOptions.setVisible(true);
            newUserOptions.setManaged(true);
            statusLabel.setText("Please select encryption options");
            statusLabel.setStyle("-fx-text-fill: blue;");
            return;
        }
        
        String username = usernameField.getText().trim();
        String password = masterPasswordField.getText();
        String algorithm = algorithmCombo.getValue();
        String mode = modeCombo.getValue();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields");
            return;
        }
        
        if (algorithm == null || mode == null) {
            statusLabel.setText("Please select algorithm and mode");
            newUserOptions.setVisible(true);
            newUserOptions.setManaged(true);
            return;
        }
        
        try {
            if (FileManager.userExists(username)) {
                statusLabel.setText("User already exists. Please login.");
                return;
            }
            
            currentUser = new User(username, password, algorithm, mode);
            statusLabel.setText("Registration successful!");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            MainCLass.setRoot("primary");
            
        } catch (Exception e) {
            statusLabel.setText("Registration failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
    
    /**
     * Returns the currently authenticated user.
     *
     * @return current user instance
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Minimizes the application window.
     */
    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setIconified(true);
    }
    
    /**
     * Closes the application.
     */
    @FXML
    private void handleClose() {
        Platform.exit();
    }
}