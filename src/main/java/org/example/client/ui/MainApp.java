//package org.example.client.ui;
//
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import org.example.client.ServerConnection;
//
///**
// * JavaFX application entry point.
// */
//public class MainApp extends Application {
//
//    private static Stage primaryStage;
//    private static ServerConnection connection;
//
//    @Override
//    public void start(Stage stage) throws Exception {
//        primaryStage = stage;
//        stage.setTitle("AppMessagerie");
//        stage.setResizable(false);
//        showLogin();
//        stage.show();
//    }
//
//    public static void showLogin() {
//        loadScene("/fxml/login.fxml", 800, 520);
//    }
//
//    public static void showRegister() {
//        loadScene("/fxml/register.fxml", 800, 560);
//    }
//
//    public static void showChat(String username, ServerConnection conn) {
//        connection = conn;
//        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/chat.fxml"));
//        try {
//            Parent root = loader.load();
//            ChatController ctrl = loader.getController();
//            ctrl.init(username, conn);
//            primaryStage.setResizable(true);
//            primaryStage.setMinWidth(750);
//            primaryStage.setMinHeight(500);
//            primaryStage.setScene(new Scene(root, 1050, 680));
//            primaryStage.setTitle("AppMessagerie – " + username);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void loadScene(String fxml, int w, int h) {
//        try {
//            Parent root = FXMLLoader.load(MainApp.class.getResource(fxml));
//            primaryStage.setResizable(false);
//            primaryStage.setScene(new Scene(root, w, h));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void stop() {
//        if (connection != null) connection.disconnect();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}

package org.example.client.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.client.ServerConnection;

/**
 * Point d'entrée de l'application JavaFX.
 */
public class MainApp extends Application {

    private static Stage            fenetrePrincipale;
    private static ServerConnection connexion;

    @Override
    public void start(Stage fenetre) throws Exception {
        fenetrePrincipale = fenetre;
        fenetre.setTitle("AppMessagerie");
        fenetre.setResizable(false);
        afficherConnexion();
        fenetre.show();
    }

    public static void afficherConnexion() {
        chargerScene("/fxml/login.fxml", 800, 520);
    }

    public static void afficherInscription() {
        chargerScene("/fxml/register.fxml", 800, 560);
    }

    public static void afficherChat(String nomUtilisateur, ServerConnection conn) {
        connexion = conn;
        FXMLLoader chargeur = new FXMLLoader(MainApp.class.getResource("/fxml/chat.fxml"));
        try {
            Parent racine = chargeur.load();
            ChatController controleur = chargeur.getController();
            controleur.initialiser(nomUtilisateur, conn);
            fenetrePrincipale.setResizable(true);
            fenetrePrincipale.setMinWidth(750);
            fenetrePrincipale.setMinHeight(500);
            fenetrePrincipale.setScene(new Scene(racine, 1050, 680));
            fenetrePrincipale.setTitle("AppMessagerie – " + nomUtilisateur);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void chargerScene(String fxml, int largeur, int hauteur) {
        try {
            Parent racine = FXMLLoader.load(MainApp.class.getResource(fxml));
            fenetrePrincipale.setResizable(false);
            fenetrePrincipale.setScene(new Scene(racine, largeur, hauteur));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (connexion != null) connexion.deconnecter();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
