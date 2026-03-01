package org.example.client.ui;

import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.example.client.ServerConnection;

public class RegisterController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         errorLabel;
    @FXML private Button        registerButton;

    private ServerConnection connection;

    @FXML
    private void handleRegister() {
        clearError();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        registerButton.setDisable(true);

        connection = new ServerConnection(this::onMessage, this::onError);
        if (!connection.connect()) {
            showError("Impossible de se connecter au serveur.");
            registerButton.setDisable(false);
            return;
        }

        connection.sendRegister(username, password);
    }

    private void onMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "REGISTER_SUCCESS" -> {
                connection.disconnect();
                showSuccess("✓  Compte créé avec succès ! Redirection…");
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(e -> MainApp.showLogin());
                pause.play();
            }
            case "REGISTER_FAIL" -> {
                showError(json.get("message").getAsString());
                registerButton.setDisable(false);
                connection.disconnect();
            }
            default -> registerButton.setDisable(false);
        }
    }

    private void onError(String msg) {
        showError(msg);
        registerButton.setDisable(false);
    }

    private void showError(String msg) {
        errorLabel.getStyleClass().removeAll("success-box");
        if (!errorLabel.getStyleClass().contains("error-box"))
            errorLabel.getStyleClass().add("error-box");
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        errorLabel.getStyleClass().removeAll("error-box");
        if (!errorLabel.getStyleClass().contains("success-box"))
            errorLabel.getStyleClass().add("success-box");
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
    private void goToLogin() {
        MainApp.showLogin();
    }
}
