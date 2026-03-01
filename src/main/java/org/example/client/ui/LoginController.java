package org.example.client.ui;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.client.ServerConnection;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private ServerConnection connection;

    @FXML
    private void handleLogin() {
        clearError();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        loginButton.setDisable(true);

        connection = new ServerConnection(this::onMessage, this::onError);
        if (!connection.connect()) {
            showError("Impossible de se connecter au serveur.\nVérifiez qu'il est démarré.");
            loginButton.setDisable(false);
            return;
        }

        connection.sendLogin(username, password);
    }

    private void onMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "LOGIN_SUCCESS" -> {
                String uname = json.get("username").getAsString();
                MainApp.showChat(uname, connection);
            }
            case "LOGIN_FAIL" -> {
                showError(json.get("message").getAsString());
                loginButton.setDisable(false);
                connection.disconnect();
            }
            default -> loginButton.setDisable(false);
        }
    }

    private void onError(String msg) {
        showError(msg);
        loginButton.setDisable(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void goToRegister() {
        MainApp.showRegister();
    }
}
