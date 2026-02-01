package com.mnemos.ui;

import com.mnemos.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class SetupPasswordController {
    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmField;

    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();
    private Stage stage;
    private boolean setupComplete = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isSetupComplete() {
        return setupComplete;
    }

    @FXML
    private void handleSetup() {
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        // Validate password
        if (password == null || password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        // Save password
        authService.setPassword(password);
        setupComplete = true;

        if (stage != null) {
            stage.close();
        }
    }
}
