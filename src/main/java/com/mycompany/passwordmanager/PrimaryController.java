package com.mycompany.passwordmanager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the main vault view.
 * Handles adding, showing, deleting passwords and updating dashboard statistics.
 */
public class PrimaryController {
    @FXML private Label welcomeLabel;
    @FXML private ListView<String> passwordListView;
    @FXML private TextField labelField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker expirationDatePicker;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private Label totalCountLabel;
    @FXML private Label expiringCountLabel;
    @FXML private Label noExpiryCountLabel;
    
    private User currentUser;
    private ObservableList<String> passwordLabels;
    private FilteredList<String> filteredPasswords;
    /**
     * Prepares bindings, loads passwords, and configures filtering.
     */
    @FXML
    private void initialize() {
        currentUser = LoginController.getCurrentUser();
        passwordLabels = FXCollections.observableArrayList();
        filteredPasswords = new FilteredList<>(passwordLabels, s -> true);
        passwordListView.setItems(filteredPasswords);
        setupListView();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> {
                String query = newValue == null ? "" : newValue.trim().toLowerCase();
                filteredPasswords.setPredicate(item -> query.isEmpty() || item.toLowerCase().contains(query));
            });
        }

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getName() + "!");
            loadPasswords();
        }
    }
    /**
     * Refreshes UI lists and statistics from the user model.
     */
    private void loadPasswords() {
        Map<String, User.PasswordEntry> passwords = currentUser.getAllPasswords();
        passwordLabels.clear();
        for (Map.Entry<String, User.PasswordEntry> entry : passwords.entrySet()) {
            String displayText = entry.getKey();
            if (entry.getValue().getUsername() != null && !entry.getValue().getUsername().isEmpty()) {
                displayText += " (" + entry.getValue().getUsername() + ")";
            }
            passwordLabels.add(displayText);
        }
        passwordListView.getSelectionModel().clearSelection();
        passwordListView.refresh();
        updateStats(passwords);
    }
    /**
     * Persists a new password entry using the provided form values.
     */    
    @FXML
    private void handleAddPassword() {
        String label = labelField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        LocalDate expirationDate = expirationDatePicker.getValue();
        
        if (label.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill label, username and password");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        try {
            currentUser.addPassword(label, username, password, expirationDate);
            loadPasswords();
            labelField.clear();
            usernameField.clear();
            passwordField.clear();
            expirationDatePicker.setValue(null);
            if (searchField != null) {
                searchField.clear();
            }
            statusLabel.setText("Password added successfully!");
            statusLabel.setStyle("-fx-text-fill: green;");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
    /**
     * Reveals the selected credential in an information dialog.
     */    
    @FXML
    private void handleShowPassword() {
        String selectedItem = passwordListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            statusLabel.setText("Please select a password");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        // Extraire le label (avant le ' (username)')
        String selectedLabel = selectedItem.split(" \\(")[0];
        User.PasswordEntry entry = currentUser.getPasswordEntry(selectedLabel);
        
        if (entry == null) {
            statusLabel.setText("Password not found");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        StringBuilder content = new StringBuilder();
        content.append("Username: ").append(entry.getUsername()).append("\n");
        content.append("Password: ").append(entry.getPassword()).append("\n");
        if (entry.getExpirationDate() != null) {
            content.append("Expiration: ").append(entry.getExpirationDate());
            long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), entry.getExpirationDate());
            if (daysUntilExpiration < 0) {
                content.append(" (EXPIRED)");
            } else if (daysUntilExpiration <= 30) {
                content.append(" (expires in ").append(daysUntilExpiration).append(" days)");
            }
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Details");
        alert.setHeaderText(selectedLabel);
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    /**
     * Deletes the selected password entry after confirmation.
     */    
    @FXML
    private void handleDelete() {
        String selectedItem = passwordListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            statusLabel.setText("Please select a password to delete");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        String selectedLabel = selectedItem.split(" \\(")[0];
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete password for: " + selectedLabel);
        confirmation.setContentText("Are you sure you want to delete this password?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean removed = currentUser.deletePassword(selectedLabel);
                if (!removed) {
                    statusLabel.setText("Password not found");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }

                loadPasswords();
                statusLabel.setText("Password deleted");
                statusLabel.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                statusLabel.setText("Deletion failed: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }
    
    /**
     * Checks and displays passwords that are expiring within a user-specified number of days.
     */
    @FXML
    private void handleCheckExpiration() {
        TextInputDialog dialog = new TextInputDialog("30");
        dialog.setTitle("Check Expiration");
        dialog.setHeaderText("Password Expiration Check");
        dialog.setContentText("Check passwords expiring within (days):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int days = Integer.parseInt(result.get());
                LocalDate threshold = LocalDate.now().plusDays(days);
                
                Map<String, User.PasswordEntry> passwords = currentUser.getAllPasswords();
                String expiringPasswords = passwords.entrySet().stream()
                    .filter(entry -> entry.getValue().getExpirationDate() != null)
                    .filter(entry -> !entry.getValue().getExpirationDate().isAfter(threshold))
                    .map(entry -> {
                        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), entry.getValue().getExpirationDate());
                        String status = daysLeft < 0 ? " (EXPIRED)" : " (" + daysLeft + " days left)";
                        return entry.getKey() + " - " + entry.getValue().getUsername() + status;
                    })
                    .collect(Collectors.joining("\n"));
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Expiring Passwords");
                alert.setHeaderText("Passwords expiring within " + days + " days:");
                alert.setContentText(expiringPasswords.isEmpty() ? "No passwords expiring soon!" : expiringPasswords);
                alert.showAndWait();
            } catch (NumberFormatException e) {
                statusLabel.setText("Invalid number of days");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }
    
    /**
     * Logs out the current user and returns to the login screen.
     */
    @FXML
    private void handleLogout() {
        try {
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            MainCLass.setRoot("login");
            Platform.runLater(() -> {
                stage.centerOnScreen();
            });
        } catch (Exception e) {
            statusLabel.setText("Logout error: " + e.getMessage());
        }
    }

    /**
     * Clears the search field input.
     */
    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    /**
     * Updates the statistics labels based on the current password entries.
     */
    private void updateStats(Map<String, User.PasswordEntry> passwords) {
        int total = passwords.size();
        long noExpiry = passwords.values().stream()
                .filter(entry -> entry.getExpirationDate() == null)
                .count();
        long expiringSoon = passwords.values().stream()
                .filter(entry -> entry.getExpirationDate() != null)
                .filter(entry -> {
                    LocalDate date = entry.getExpirationDate();
                    LocalDate now = LocalDate.now();
                    return !date.isBefore(now) && !date.isAfter(now.plusDays(30));
                })
                .count();

        if (totalCountLabel != null) {
            totalCountLabel.setText(Integer.toString(total));
        }
        if (noExpiryCountLabel != null) {
            noExpiryCountLabel.setText(Long.toString(noExpiry));
        }
        if (expiringCountLabel != null) {
            expiringCountLabel.setText(Long.toString(expiringSoon));
        }
    }
    
    /**
     * Minimizes the application window.
     */
    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.setIconified(true);
    }
    
    /**
     * Closes the application.
     */
    @FXML
    private void handleClose() {
        Platform.exit();
    }

    /**
     * Configures the ListView with custom cell rendering.
     */
    private void setupListView() {
        passwordListView.setCellFactory(listView -> new VaultCell());
    }
    
    /**
     * Extracts the label part from a display text string.
     *
     * @param displayText the full display text
     * @return the extracted label or null if input is null
     */
    private String extractLabel(String displayText) {
        if (displayText == null) {
            return null;
        }
        int index = displayText.indexOf(" (");
        return index >= 0 ? displayText.substring(0, index) : displayText;
    }
    /**
     * Determines the status information for a password entry based on its expiration date.
     *
     * @param entry the password entry
     * @return status information including text and colors
     */
    private StatusInfo getStatusInfo(User.PasswordEntry entry) {
        if (entry == null) {
            return new StatusInfo("No data", "rgba(148,163,184,0.9)", "rgba(148,163,184,0.2)", "rgba(226,232,240,0.95)");
        }

        LocalDate expiration = entry.getExpirationDate();
        if (expiration == null) {
            return new StatusInfo("No expiration", "#38bdf8", "rgba(56,189,248,0.18)", "#bae6fd");
        }

        long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expiration);
        if (daysUntilExpiration < 0) {
            return new StatusInfo("Expired", "#f87171", "rgba(248,113,113,0.22)", "#fecaca");
        }
        if (daysUntilExpiration <= 7) {
            return new StatusInfo("Expires in " + daysUntilExpiration + " d", "#fbbf24", "rgba(251,191,36,0.22)", "#fde68a");
        }
        if (daysUntilExpiration <= 30) {
            return new StatusInfo("Expires soon", "#facc15", "rgba(250,204,21,0.22)", "#fef08a");
        }

        return new StatusInfo("Healthy", "#34d399", "rgba(52,211,153,0.18)", "#bbf7d0");
    }
    
    /**
     * Returns the CSS style string for a status dot with the given color.
     *
     * @param color the color of the dot
     * @return CSS style string
     */
    private String getDotStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-background-radius: 999; -fx-min-width: 8px; -fx-min-height: 8px; -fx-max-width: 8px; -fx-max-height: 8px;";
    }
    /**
     * Returns the CSS style string for a ListView cell based on its selection state.
     *
     * @param selected whether the cell is selected
     * @return CSS style string
     */
    private String getCellBackground(boolean selected) {
        String background = selected ? "rgba(99,102,241,0.35)" : "rgba(30,41,59,0.88)";
        String border = selected ? "rgba(129,140,248,0.55)" : "rgba(148,163,184,0.18)";
        return "-fx-background-color: " + background + "; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-width: 1; -fx-border-color: " + border + "; -fx-padding: 14 18 14 20; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.55), 20, 0.25, 0, 10);";
    }
    
    /**
     * Holds status information for a password entry.
     */
    private static class StatusInfo {
        private final String text;
        private final String accentColor;
        private final String backgroundColor;
        private final String labelTextColor;
        /**
         * Creates a new status descriptor.
         *
         * @param text display text
         * @param accentColor dot color
         * @param backgroundColor badge background
         * @param labelTextColor badge text color
         */
        StatusInfo(String text, String accentColor, String backgroundColor, String labelTextColor) {
            this.text = text;
            this.accentColor = accentColor;
            this.backgroundColor = backgroundColor;
            this.labelTextColor = labelTextColor;
        }
        
        /**
         * Returns the display text.
         *
         * @return display text
         */
        String text() {
            return text;
        }
        
        /**
         * Returns the accent color.
         *
         * @return accent color
         */
        String accentColor() {
            return accentColor;
        }
        /**
         * Returns the background color.
         *
         * @return background color
         */
        String backgroundColor() {
            return backgroundColor;
        }

        /**
         * Returns the label text color.
         *
         * @return label text color
         */
        String labelTextColor() {
            return labelTextColor;
        }
    }
    /**
     * Formats the badge style based on {@link StatusInfo}.
     *
     * @param info status descriptor
     * @return CSS style string
     */    
    private String getStatusStyle(StatusInfo info) {
        return "-fx-background-color: " + info.backgroundColor() + "; -fx-text-fill: " + info.labelTextColor()
                + "; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 999; -fx-padding: 4 12;";
    }
    /**
     * Custom list cell that displays credential metadata with status indicators.
     */
    private class VaultCell extends ListCell<String> {
        private final Label titleLabel = new Label();
        private final Label subtitleLabel = new Label();
        private final Label statusLabel = new Label();
        private final Region statusDot = new Region();
        private final HBox root;
        
        /**
         * Constructs a new VaultCell.
         */
        VaultCell() {
            titleLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
            subtitleLabel.setStyle("-fx-text-fill: rgba(148,163,184,0.85); -fx-font-size: 11px; -fx-font-weight: 500;");
            statusLabel.setStyle(getStatusStyle(new StatusInfo("", "rgba(148,163,184,0.9)", "rgba(148,163,184,0.18)", "rgba(226,232,240,0.9)")));
            statusDot.setStyle(getDotStyle("rgba(148,163,184,0.9)"));

            VBox textBox = new VBox(4, titleLabel, subtitleLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);

            HBox statusBox = new HBox(6, statusDot, statusLabel);
            statusBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            root = new HBox(14, textBox, spacer, statusBox);
            root.setAlignment(Pos.CENTER_LEFT);

            setText(null);
        }
        /**
         * Refreshes cell content when items or selection change.
         *
         * @param item display text
         * @param empty {@code true} if the cell should be cleared
         */
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String label = extractLabel(item);
            titleLabel.setText(label != null ? label : "—");

            User.PasswordEntry entry = currentUser != null && label != null
                    ? currentUser.getPasswordEntry(label)
                    : null;

            String usernameText = entry != null && entry.getUsername() != null && !entry.getUsername().isEmpty()
                    ? entry.getUsername()
                    : "No username";
            subtitleLabel.setText(usernameText);

            StatusInfo statusInfo = getStatusInfo(entry);
            statusLabel.setText(statusInfo.text().toUpperCase());
            statusLabel.setStyle(getStatusStyle(statusInfo));
            statusDot.setStyle(getDotStyle(statusInfo.accentColor()));

            root.setStyle(getCellBackground(isSelected()));
            setGraphic(root);
        }
        /**
         * Ensures visual state follows selection changes.
         *
         * @param selected selection flag
         */
        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            root.setStyle(getCellBackground(selected));
        }
    }
}