//package org.example.client;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import javafx.application.Platform;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.function.Consumer;
//
///**
// * Manages the TCP connection to the server.
// * All incoming messages are dispatched to a listener on the JavaFX thread.
// */
//public class ServerConnection {
//
//    private static final Logger log = LoggerFactory.getLogger(ServerConnection.class);
//
//    private static final String HOST = "localhost";
//    private static final int    PORT = 5000;
//
//    private Socket socket;
//    private PrintWriter out;
//    private Consumer<JsonObject> messageListener;
//    private Consumer<String>     errorListener;
//
//    public ServerConnection(Consumer<JsonObject> messageListener, Consumer<String> errorListener) {
//        this.messageListener = messageListener;
//        this.errorListener   = errorListener;
//    }
//
//    public boolean connect() {
//        try {
//            socket = new Socket(HOST, PORT);
//            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
//
//            // Listen for incoming messages in a daemon thread
//            Thread reader = new Thread(this::readLoop);
//            reader.setDaemon(true);
//            reader.start();
//            return true;
//        } catch (IOException e) {
//            log.error("Impossible de se connecter au serveur : {}", e.getMessage());
//            return false;
//        }
//    }
//
//    private void readLoop() {
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = in.readLine()) != null) {
//                final JsonObject json = JsonParser.parseString(line).getAsJsonObject();
//                Platform.runLater(() -> messageListener.accept(json));
//            }
//        } catch (IOException e) {
//            log.warn("Connexion au serveur perdue : {}", e.getMessage());
//            // RG10
//            Platform.runLater(() -> errorListener.accept(
//                    "Connexion au serveur perdue. Vous êtes hors ligne."));
//        }
//    }
//
//    public void send(JsonObject obj) {
//        if (out != null) out.println(obj.toString());
//    }
//
//    /** Allow ChatController to replace the listeners after login. */
//    public void setMessageListener(Consumer<JsonObject> listener) {
//        this.messageListener = listener;
//    }
//
//    public void setErrorListener(Consumer<String> listener) {
//        this.errorListener = listener;
//    }
//
//    public void disconnect() {
//        try {
//            if (socket != null && !socket.isClosed()) socket.close();
//        } catch (IOException ignored) {}
//    }
//
//    // -------------------- convenience builders --------------------
//
//    public void sendLogin(String username, String password) {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "LOGIN");
//        req.addProperty("username", username);
//        req.addProperty("password", password);
//        send(req);
//    }
//
//    public void sendRegister(String username, String password) {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "REGISTER");
//        req.addProperty("username", username);
//        req.addProperty("password", password);
//        send(req);
//    }
//
//    public void sendMessage(String receiver, String content) {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "SEND_MESSAGE");
//        req.addProperty("receiver", receiver);
//        req.addProperty("content", content);
//        send(req);
//    }
//
//    public void requestHistory(String otherUser) {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "GET_HISTORY");
//        req.addProperty("otherUser", otherUser);
//        send(req);
//    }
//
//    public void requestUserList() {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "GET_USERS");
//        send(req);
//    }
//
//    public void sendLogout() {
//        JsonObject req = new JsonObject();
//        req.addProperty("type", "LOGOUT");
//        send(req);
//        disconnect();
//    }
//}

package org.example.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gère la connexion TCP au serveur.
 * Tous les messages entrants sont transmis à un écouteur sur le thread JavaFX.
 */
public class ServerConnection {

    private static final Logger journal = LoggerFactory.getLogger(ServerConnection.class);

    private static final String HOTE = "localhost";
    private static final int    PORT = 5000;

    private Socket socket;
    private PrintWriter sortie;
    private Consumer<JsonObject> ecouteurMessages;
    private Consumer<String>     ecouteurErreurs;

    // Flag pour distinguer une déconnexion volontaire d'une perte de connexion
    private volatile boolean deconnexionVolontaire = false;

    public ServerConnection(Consumer<JsonObject> ecouteurMessages, Consumer<String> ecouteurErreurs) {
        this.ecouteurMessages = ecouteurMessages;
        this.ecouteurErreurs  = ecouteurErreurs;
    }

    public boolean connecter() {
        try {
            deconnexionVolontaire = false;
            socket = new Socket(HOTE, PORT);
            sortie = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Écouter les messages entrants dans un thread démon
            Thread lecteur = new Thread(this::bouctureLecture);
            lecteur.setDaemon(true);
            lecteur.start();
            return true;
        } catch (IOException e) {
            journal.error("Impossible de se connecter au serveur : {}", e.getMessage());
            return false;
        }
    }

    private void bouctureLecture() {
        try (BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String ligne;
            while ((ligne = entree.readLine()) != null) {
                final JsonObject json = JsonParser.parseString(ligne).getAsJsonObject();
                Platform.runLater(() -> ecouteurMessages.accept(json));
            }
        } catch (IOException e) {
            journal.warn("Connexion au serveur perdue : {}", e.getMessage());
            // RG10 — notifier uniquement si la déconnexion n'est pas volontaire
            if (!deconnexionVolontaire) {
                Platform.runLater(() -> ecouteurErreurs.accept(
                        "Connexion au serveur perdue. Vous êtes hors ligne."));
            }
        }
    }

    public void envoyer(JsonObject obj) {
        if (sortie != null) sortie.println(obj.toString());
    }

    /** Permet au ChatController de remplacer les écouteurs après la connexion. */
    public void setMessageListener(Consumer<JsonObject> ecouteur) {
        this.ecouteurMessages = ecouteur;
    }

    public void setErrorListener(Consumer<String> ecouteur) {
        this.ecouteurErreurs = ecouteur;
    }

    public void deconnecter() {
        deconnexionVolontaire = true;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignore) {}
    }

    // -------------------- constructeurs de commodité --------------------

    public void sendLogin(String nomUtilisateur, String motDePasse) {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "LOGIN");
        requete.addProperty("username", nomUtilisateur);
        requete.addProperty("password", motDePasse);
        envoyer(requete);
    }

    public void sendRegister(String nomUtilisateur, String motDePasse) {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "REGISTER");
        requete.addProperty("username", nomUtilisateur);
        requete.addProperty("password", motDePasse);
        envoyer(requete);
    }

    public void sendMessage(String destinataire, String contenu) {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "SEND_MESSAGE");
        requete.addProperty("receiver", destinataire);
        requete.addProperty("content", contenu);
        envoyer(requete);
    }

    public void requestHistory(String autreUtilisateur) {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "GET_HISTORY");
        requete.addProperty("otherUser", autreUtilisateur);
        envoyer(requete);
    }

    public void requestUserList() {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "GET_USERS");
        envoyer(requete);
    }

    public void sendLogout() {
        JsonObject requete = new JsonObject();
        requete.addProperty("type", "LOGOUT");
        envoyer(requete);
        deconnecter();
    }
}