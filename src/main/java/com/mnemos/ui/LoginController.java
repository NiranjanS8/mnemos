package com.mnemos.ui;

import com.mnemos.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();
    private Stage stage;
    private boolean loginSuccess = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    @FXML
    private void handleLogin() {
        String password = passwordField.getText();

        if (authService.verifyPassword(password)) {
            loginSuccess = true;
            if (stage != null) {
                stage.close();
            }
        } else {
            errorLabel.setText("Incorrect password");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
}
