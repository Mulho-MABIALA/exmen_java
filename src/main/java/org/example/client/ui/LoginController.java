//package org.example.client.ui;
//
//import com.google.gson.JsonObject;
//import javafx.fxml.FXML;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.PasswordField;
//import javafx.scene.control.TextField;
//import org.example.client.ServerConnection;
//
//public class LoginController {
//
//    @FXML private TextField     usernameField;
//    @FXML private PasswordField passwordField;
//    @FXML private Label         errorLabel;
//    @FXML private Button        loginButton;
//
//    private ServerConnection connection;
//
//    @FXML
//    private void handleLogin() {
//        clearError();
//        String username = usernameField.getText().trim();
//        String password = passwordField.getText();
//
//        if (username.isEmpty() || password.isEmpty()) {
//            showError("Veuillez remplir tous les champs.");
//            return;
//        }
//
//        loginButton.setDisable(true);
//
//        connection = new ServerConnection(this::onMessage, this::onError);
//        if (!connection.connect()) {
//            showError("Impossible de se connecter au serveur.\nVérifiez qu'il est démarré.");
//            loginButton.setDisable(false);
//            return;
//        }
//
//        connection.sendLogin(username, password);
//    }
//
//    private void onMessage(JsonObject json) {
//        String type = json.get("type").getAsString();
//        switch (type) {
//            case "LOGIN_SUCCESS" -> {
//                String uname = json.get("username").getAsString();
//                MainApp.showChat(uname, connection);
//            }
//            case "LOGIN_FAIL" -> {
//                showError(json.get("message").getAsString());
//                loginButton.setDisable(false);
//                connection.disconnect();
//            }
//            default -> loginButton.setDisable(false);
//        }
//    }
//
//    private void onError(String msg) {
//        showError(msg);
//        loginButton.setDisable(false);
//    }
//
//    private void showError(String msg) {
//        errorLabel.setText(msg);
//        errorLabel.setVisible(true);
//        errorLabel.setManaged(true);
//    }
//
//    private void clearError() {
//        errorLabel.setText("");
//        errorLabel.setVisible(false);
//        errorLabel.setManaged(false);
//    }
//
//    @FXML
//    private void goToRegister() {
//        MainApp.showRegister();
//    }
//}

package org.example.client.ui;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.example.client.ServerConnection;

public class LoginController {

    @FXML private TextField     champNomUtilisateur;
    @FXML private PasswordField champMotDePasse;
    @FXML private Label         labelErreur;
    @FXML private Button        boutonConnexion;

    private ServerConnection connexion;

    @FXML
    private void gererConnexion() {
        effacerErreur();
        String nomUtilisateur = champNomUtilisateur.getText().trim();
        String motDePasse     = champMotDePasse.getText();

        if (nomUtilisateur.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        boutonConnexion.setDisable(true);

        connexion = new ServerConnection(this::surReceptionMessage, this::surErreur);
        if (!connexion.connecter()) {
            afficherErreur("Impossible de se connecter au serveur.\nVérifiez qu'il est démarré.");
            boutonConnexion.setDisable(false);
            return;
        }

        connexion.sendLogin(nomUtilisateur, motDePasse);
    }

    private void surReceptionMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "LOGIN_SUCCESS" -> {
                String nomU = json.get("username").getAsString();
                MainApp.afficherChat(nomU, connexion);
            }
            case "LOGIN_FAIL" -> {
                afficherErreur(json.get("message").getAsString());
                boutonConnexion.setDisable(false);
                connexion.deconnecter();
            }
            default -> boutonConnexion.setDisable(false);
        }
    }

    private void surErreur(String message) {
        afficherErreur(message);
        boutonConnexion.setDisable(false);
    }

    private void afficherErreur(String message) {
        labelErreur.setText(message);
        labelErreur.setVisible(true);
        labelErreur.setManaged(true);
    }

    private void effacerErreur() {
        labelErreur.setText("");
        labelErreur.setVisible(false);
        labelErreur.setManaged(false);
    }

    @FXML
    private void allerVersInscription() {
        MainApp.afficherInscription();
    }
}
