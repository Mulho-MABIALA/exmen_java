//package org.example.client.ui;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import javafx.fxml.FXML;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.StackPane;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Circle;
//import org.example.client.ServerConnection;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class ChatController {
//
//    @FXML private Label          currentUserLabel;
//    @FXML private Label          chatWithLabel;
//    @FXML private Label          chatStatusLabel;
//    @FXML private StackPane      contactAvatar;
//    @FXML private ListView<HBox> messageListView;
//    @FXML private ListView<HBox> userListView;
//    @FXML private TextField      messageField;
//
//    private String           currentUser;
//    private String           selectedUser;
//    private ServerConnection connection;
//
//    // Stocker les labels meta des messages envoyés pour les mettre à jour
//    // clé = "sender:content:time" → label meta
//    private final Map<Long, Label> sentMetaLabels = new HashMap<>();
//
//    private static final String[] AVATAR_COLORS = {
//            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
//            "#3F51B5", "#2196F3", "#009688", "#4CAF50",
//            "#FF9800", "#FF5722", "#795548", "#607D8B"
//    };
//
//    public void init(String username, ServerConnection conn) {
//        this.currentUser = username;
//        this.connection  = conn;
//        currentUserLabel.setText(username);
//        conn.setMessageListener(this::onMessage);
//        conn.setErrorListener(this::onError);
//
//        userListView.setOnMouseClicked(e -> {
//            HBox sel = userListView.getSelectionModel().getSelectedItem();
//            if (sel != null) {
//                String uname = (String) sel.getUserData();
//                if (uname != null && !uname.equals(currentUser)) openConversation(uname);
//            }
//        });
//
//        messageField.setOnAction(e -> handleSend());
//        conn.requestUserList();
//    }
//
//    private void openConversation(String uname) {
//        selectedUser = uname;
//        chatWithLabel.setText(uname);
//        chatStatusLabel.setText("chargement...");
//        messageListView.getItems().clear();
//        sentMetaLabels.clear();
//        contactAvatar.getChildren().setAll(buildAvatarCircle(uname, 20));
//        connection.requestHistory(uname);
//    }
//
//    @FXML
//    private void handleSend() {
//        if (selectedUser == null) { showAlert("Selectionnez un utilisateur."); return; }
//        String text = messageField.getText().trim();
//        if (text.isEmpty()) return;
//        if (text.length() > 1000) { showAlert("Max 1000 caracteres."); return; }
//        connection.sendMessage(selectedUser, text);
//        messageField.clear();
//    }
//
//    private void onMessage(JsonObject json) {
//        String type = json.get("type").getAsString();
//        switch (type) {
//            case "USER_LIST"        -> handleUserList(json);
//            case "MESSAGE"          -> handleIncomingMessage(json);
//            case "MESSAGE_SENT"     -> handleSentConfirmation(json);
//            case "HISTORY"          -> handleHistory(json);
//            case "MESSAGE_DELIVERED"-> handleMessageDelivered(json);
//            case "ERROR"            -> showAlert(json.get("message").getAsString());
//        }
//    }
//
//    private void handleUserList(JsonObject json) {
//        userListView.getItems().clear();
//        JsonArray users = json.getAsJsonArray("users");
//        for (int i = 0; i < users.size(); i++) {
//            JsonObject u     = users.get(i).getAsJsonObject();
//            String uname     = u.get("username").getAsString();
//            boolean isOnline = "ONLINE".equals(u.get("status").getAsString());
//            boolean isMe     = uname.equals(currentUser);
//            boolean isSel    = uname.equals(selectedUser);
//
//            HBox row = new HBox(12);
//            row.setAlignment(Pos.CENTER_LEFT);
//            row.setPadding(new Insets(10, 16, 10, 14));
//            row.setUserData(uname);
//            if (isSel) {
//                row.setStyle("-fx-background-color: #E8F5E9; -fx-cursor: hand;"
//                        + " -fx-border-color: #00A884 transparent transparent transparent;"
//                        + " -fx-border-width: 0 0 0 3;");
//            } else {
//                row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
//            }
//
//            VBox info = new VBox(2);
//            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
//
//            Label nameLabel = new Label(uname + (isMe ? "  (moi)" : ""));
//            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111B21;"
//                    + (isMe ? " -fx-font-style: italic;" : ""));
//
//            Label statusLabel = new Label(isOnline ? "En ligne" : "Hors ligne");
//            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: "
//                    + (isOnline ? "#00A884;" : "#8696A0;"));
//
//            info.getChildren().addAll(nameLabel, statusLabel);
//
//            Circle dot = new Circle(4);
//            dot.setFill(Color.web(isOnline ? "#25D366" : "#B0BEC5"));
//
//            row.getChildren().addAll(buildAvatarCircle(uname, 20), info, dot);
//            userListView.getItems().add(row);
//
//            if (isSel) chatStatusLabel.setText(isOnline ? "En ligne" : "Hors ligne");
//        }
//    }
//
//    private void handleIncomingMessage(JsonObject json) {
//        String sender = json.get("sender").getAsString();
//        if (sender.equals(selectedUser))
//            appendMessage(
//                    sender,
//                    json.get("content").getAsString(),
//                    json.get("dateEnvoi").getAsString(),
//                    false,
//                    "RECU",
//                    null
//            );
//    }
//
//    private void handleSentConfirmation(JsonObject json) {
//        // Le serveur indique si le destinataire était en ligne (RECU) ou non (ENVOYE)
//        String statut = json.has("statut") ? json.get("statut").getAsString() : "ENVOYE";
//        Long   msgId  = json.has("id")     ? json.get("id").getAsLong()        : null;
//
//        appendMessage(
//                currentUser,
//                json.get("content").getAsString(),
//                json.get("dateEnvoi").getAsString(),
//                true,
//                statut,
//                msgId
//        );
//    }
//
//    private void handleHistory(JsonObject json) {
//        messageListView.getItems().clear();
//        sentMetaLabels.clear();
//        chatStatusLabel.setText("");
//        JsonArray messages = json.getAsJsonArray("messages");
//        for (int i = 0; i < messages.size(); i++) {
//            JsonObject m   = messages.get(i).getAsJsonObject();
//            boolean isMine = m.get("sender").getAsString().equals(currentUser);
//            String statut  = m.has("statut") ? m.get("statut").getAsString() : "ENVOYE";
//            Long   msgId   = m.has("id")     ? m.get("id").getAsLong()        : null;
//
//            appendMessage(
//                    m.get("sender").getAsString(),
//                    m.get("content").getAsString(),
//                    m.get("dateEnvoi").getAsString(),
//                    isMine,
//                    statut,
//                    msgId
//            );
//        }
//    }
//
//    /**
//     * Reçu lorsque le serveur notifie que le destinataire a bien reçu le message
//     * (il vient de se connecter et ses messages en attente lui ont été livrés).
//     */
//    private void handleMessageDelivered(JsonObject json) {
//        if (!json.has("id")) return;
//        Long msgId = json.get("id").getAsLong();
//        Label meta = sentMetaLabels.get(msgId);
//        if (meta != null) {
//            // Mettre à jour l'affichage : ✓ → ✓✓
//            String current = meta.getText();
//            // Remplace ✓ seul par ✓✓ (ne remplace pas si déjà ✓✓)
//            if (current.endsWith(" \u2713") && !current.endsWith(" \u2713\u2713")) {
//                meta.setText(current.replace(" \u2713", " \u2713\u2713"));
//                meta.setStyle("-fx-font-size: 10px; -fx-text-fill: #53BDEB; -fx-padding: 1 4 0 4;");
//            }
//        }
//    }
//
//    /**
//     * @param statut  "ENVOYE" → ✓ (gris), "RECU" → ✓✓ (bleu)
//     * @param msgId   ID du message pour le mettre à jour plus tard si besoin
//     */
//    private void appendMessage(String sender, String content, String date,
//                               boolean isMine, String statut, Long msgId) {
//        Label bubble = new Label(content);
//        bubble.setWrapText(true);
//        bubble.setMaxWidth(460);
//        bubble.setPadding(new Insets(9, 14, 9, 14));
//        if (isMine) {
//            bubble.setStyle("-fx-background-color: #D9FDD3;"
//                    + " -fx-background-radius: 16 2 16 16; -fx-font-size: 13.5px;"
//                    + " -fx-text-fill: #111B21;"
//                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
//        } else {
//            bubble.setStyle("-fx-background-color: white;"
//                    + " -fx-background-radius: 2 16 16 16; -fx-font-size: 13.5px;"
//                    + " -fx-text-fill: #111B21;"
//                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
//        }
//
//        String timeOnly = date.length() >= 16 ? date.substring(11, 16) : date;
//
//        // ✓ = ENVOYE (message en attente, destinataire hors ligne)
//        // ✓✓ = RECU   (message reçu, destinataire en ligne ou connecté depuis)
//        String ticks = "";
//        String tickColor = "#8696A0"; // gris par défaut
//        if (isMine) {
//            if ("RECU".equals(statut)) {
//                ticks = "  \u2713\u2713"; // ✓✓ bleu
//                tickColor = "#53BDEB";
//            } else {
//                ticks = "  \u2713";      // ✓ gris
//            }
//        }
//
//        Label meta = new Label(timeOnly + ticks);
//        meta.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tickColor + "; -fx-padding: 1 4 0 4;");
//
//        // Stocker le label pour mise à jour future
//        if (isMine && msgId != null) {
//            sentMetaLabels.put(msgId, meta);
//        }
//
//        VBox box = new VBox(2, bubble, meta);
//        box.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//
//        HBox row = new HBox();
//        row.setPadding(new Insets(3, 16, 3, 16));
//        row.setStyle("-fx-background-color: transparent;");
//
//        if (isMine) {
//            row.setAlignment(Pos.CENTER_RIGHT);
//            row.getChildren().add(box);
//        } else {
//            row.setAlignment(Pos.CENTER_LEFT);
//            row.setSpacing(8);
//            row.getChildren().addAll(buildAvatarCircle(sender, 14), box);
//        }
//
//        messageListView.getItems().add(row);
//        messageListView.scrollTo(messageListView.getItems().size() - 1);
//    }
//
//    private StackPane buildAvatarCircle(String name, double radius) {
//        String color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
//        Circle circle = new Circle(radius);
//        circle.setFill(Color.web(color));
//        Label initial = new Label(name.substring(0, 1).toUpperCase());
//        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
//                + (int)(radius * 0.85) + "px;");
//        StackPane sp = new StackPane(circle, initial);
//        sp.setMinWidth(radius * 2);
//        sp.setMinHeight(radius * 2);
//        return sp;
//    }
//
//    private void onError(String msg) { showAlert(msg); }
//
//    private void showAlert(String msg) {
//        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
//        alert.setHeaderText(null);
//        alert.showAndWait();
//    }
//
//    @FXML
//    private void handleLogout() {
//        connection.sendLogout();
//        MainApp.showLogin();
//    }
//}

package org.example.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.client.ServerConnection;

import java.util.HashMap;
import java.util.Map;

public class ChatController {

    @FXML private Label          labelUtilisateurCourant;
    @FXML private Label          labelDiscussionAvec;
    @FXML private Label          labelStatutDiscussion;
    @FXML private StackPane      avatarContact;
    @FXML private ListView<HBox> listeVueMessages;
    @FXML private ListView<HBox> listeVueUtilisateurs;
    @FXML private TextField      champMessage;

    private String           utilisateurCourant;
    private String           utilisateurSelectionne;
    private ServerConnection connexion;

    // Stocker les labels méta des messages envoyés pour les mettre à jour ultérieurement
    // clé = identifiant du message → label méta
    private final Map<Long, Label> labelsMetaEnvoyes = new HashMap<>();

    private static final String[] COULEURS_AVATAR = {
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#009688", "#4CAF50",
            "#FF9800", "#FF5722", "#795548", "#607D8B"
    };

    public void initialiser(String nomUtilisateur, ServerConnection conn) {
        this.utilisateurCourant = nomUtilisateur;
        this.connexion          = conn;
        labelUtilisateurCourant.setText(nomUtilisateur);
        conn.setMessageListener(this::surReceptionMessage);
        conn.setErrorListener(this::surErreur);

        listeVueUtilisateurs.setOnMouseClicked(e -> {
            HBox selection = listeVueUtilisateurs.getSelectionModel().getSelectedItem();
            if (selection != null) {
                String nomU = (String) selection.getUserData();
                if (nomU != null && !nomU.equals(utilisateurCourant)) ouvrirConversation(nomU);
            }
        });

        champMessage.setOnAction(e -> gererEnvoi());
        conn.requestUserList();
    }

    private void ouvrirConversation(String nomU) {
        utilisateurSelectionne = nomU;
        labelDiscussionAvec.setText(nomU);
        labelStatutDiscussion.setText("chargement...");
        listeVueMessages.getItems().clear();
        labelsMetaEnvoyes.clear();
        avatarContact.getChildren().setAll(construireAvatarCercle(nomU, 20));
        connexion.requestHistory(nomU);
    }

    @FXML
    private void gererEnvoi() {
        if (utilisateurSelectionne == null) { afficherAlerte("Sélectionnez un utilisateur."); return; }
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;
        if (texte.length() > 1000) { afficherAlerte("Maximum 1000 caractères."); return; }
        connexion.sendMessage(utilisateurSelectionne, texte);
        champMessage.clear();
    }

    private void surReceptionMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "USER_LIST"         -> gererListeUtilisateurs(json);
            case "MESSAGE"           -> gererMessageEntrant(json);
            case "MESSAGE_SENT"      -> gererConfirmationEnvoi(json);
            case "HISTORY"           -> gererHistorique(json);
            case "MESSAGE_DELIVERED" -> gererMessageLivre(json);
            case "ERROR"             -> afficherAlerte(json.get("message").getAsString());
        }
    }

    private void gererListeUtilisateurs(JsonObject json) {
        listeVueUtilisateurs.getItems().clear();
        JsonArray utilisateurs = json.getAsJsonArray("users");
        for (int i = 0; i < utilisateurs.size(); i++) {
            JsonObject u       = utilisateurs.get(i).getAsJsonObject();
            String nomU        = u.get("username").getAsString();
            boolean estEnLigne = "EN_LIGNE".equals(u.get("status").getAsString());
            boolean cEstMoi    = nomU.equals(utilisateurCourant);
            boolean estSelec   = nomU.equals(utilisateurSelectionne);

            HBox ligne = new HBox(12);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setPadding(new Insets(10, 16, 10, 14));
            ligne.setUserData(nomU);
            if (estSelec) {
                ligne.setStyle("-fx-background-color: #E8F5E9; -fx-cursor: hand;"
                        + " -fx-border-color: #00A884 transparent transparent transparent;"
                        + " -fx-border-width: 0 0 0 3;");
            } else {
                ligne.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            }

            VBox infos = new VBox(2);
            HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);

            Label labelNom = new Label(nomU + (cEstMoi ? "  (moi)" : ""));
            labelNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111B21;"
                    + (cEstMoi ? " -fx-font-style: italic;" : ""));

            Label labelStatut = new Label(estEnLigne ? "En ligne" : "Hors ligne");
            labelStatut.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                    + (estEnLigne ? "#00A884;" : "#8696A0;"));

            infos.getChildren().addAll(labelNom, labelStatut);

            Circle point = new Circle(4);
            point.setFill(Color.web(estEnLigne ? "#25D366" : "#B0BEC5"));

            ligne.getChildren().addAll(construireAvatarCercle(nomU, 20), infos, point);
            listeVueUtilisateurs.getItems().add(ligne);

            if (estSelec) labelStatutDiscussion.setText(estEnLigne ? "En ligne" : "Hors ligne");
        }
    }

    private void gererMessageEntrant(JsonObject json) {
        String expediteur = json.get("sender").getAsString();
        if (expediteur.equals(utilisateurSelectionne))
            ajouterMessage(
                    expediteur,
                    json.get("content").getAsString(),
                    json.get("dateEnvoi").getAsString(),
                    false,
                    "RECU",
                    null
            );
    }

    private void gererConfirmationEnvoi(JsonObject json) {
        // Le serveur indique si le destinataire était en ligne (RECU) ou non (ENVOYE)
        String statut = json.has("statut") ? json.get("statut").getAsString() : "ENVOYE";
        Long   idMsg  = json.has("id")     ? json.get("id").getAsLong()        : null;

        ajouterMessage(
                utilisateurCourant,
                json.get("content").getAsString(),
                json.get("dateEnvoi").getAsString(),
                true,
                statut,
                idMsg
        );
    }

    private void gererHistorique(JsonObject json) {
        listeVueMessages.getItems().clear();
        labelsMetaEnvoyes.clear();
        labelStatutDiscussion.setText("");
        JsonArray messages = json.getAsJsonArray("messages");
        for (int i = 0; i < messages.size(); i++) {
            JsonObject m    = messages.get(i).getAsJsonObject();
            boolean cEstMoi = m.get("sender").getAsString().equals(utilisateurCourant);
            String statut   = m.has("statut") ? m.get("statut").getAsString() : "ENVOYE";
            Long   idMsg    = m.has("id")     ? m.get("id").getAsLong()        : null;

            ajouterMessage(
                    m.get("sender").getAsString(),
                    m.get("content").getAsString(),
                    m.get("dateEnvoi").getAsString(),
                    cEstMoi,
                    statut,
                    idMsg
            );
        }
    }

    /**
     * Reçu lorsque le serveur notifie que le destinataire a bien reçu le message
     * (il vient de se connecter et ses messages en attente lui ont été livrés).
     */
    private void gererMessageLivre(JsonObject json) {
        if (!json.has("id")) return;
        Long idMsg    = json.get("id").getAsLong();
        Label labelMeta = labelsMetaEnvoyes.get(idMsg);
        if (labelMeta != null) {
            // Mettre à jour l'affichage : ✓ → ✓✓
            String contenuActuel = labelMeta.getText();
            // Remplace ✓ seul par ✓✓ (ne remplace pas si déjà ✓✓)
            if (contenuActuel.endsWith(" \u2713") && !contenuActuel.endsWith(" \u2713\u2713")) {
                labelMeta.setText(contenuActuel.replace(" \u2713", " \u2713\u2713"));
                labelMeta.setStyle("-fx-font-size: 10px; -fx-text-fill: #53BDEB; -fx-padding: 1 4 0 4;");
            }
        }
    }

    /**
     * @param statut  "ENVOYE" → ✓ (gris), "RECU" → ✓✓ (bleu)
     * @param idMsg   Identifiant du message pour mise à jour ultérieure si nécessaire
     */
    private void ajouterMessage(String expediteur, String contenu, String date,
                                boolean cEstMoi, String statut, Long idMsg) {
        Label bulle = new Label(contenu);
        bulle.setWrapText(true);
        bulle.setMaxWidth(460);
        bulle.setPadding(new Insets(9, 14, 9, 14));
        if (cEstMoi) {
            bulle.setStyle("-fx-background-color: #D9FDD3;"
                    + " -fx-background-radius: 16 2 16 16; -fx-font-size: 13.5px;"
                    + " -fx-text-fill: #111B21;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        } else {
            bulle.setStyle("-fx-background-color: white;"
                    + " -fx-background-radius: 2 16 16 16; -fx-font-size: 13.5px;"
                    + " -fx-text-fill: #111B21;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        }

        String heureSeule = date.length() >= 16 ? date.substring(11, 16) : date;

        // ✓  = ENVOYE (message en attente, destinataire hors ligne)
        // ✓✓ = RECU   (message reçu, destinataire en ligne ou connecté depuis)
        String coches     = "";
        String couleurCoche = "#8696A0"; // gris par défaut
        if (cEstMoi) {
            if ("RECU".equals(statut)) {
                coches      = "  \u2713\u2713"; // ✓✓ bleu
                couleurCoche = "#53BDEB";
            } else {
                coches = "  \u2713";             // ✓ gris
            }
        }

        Label labelMeta = new Label(heureSeule + coches);
        labelMeta.setStyle("-fx-font-size: 10px; -fx-text-fill: " + couleurCoche + "; -fx-padding: 1 4 0 4;");

        // Stocker le label pour mise à jour future
        if (cEstMoi && idMsg != null) {
            labelsMetaEnvoyes.put(idMsg, labelMeta);
        }

        VBox boite = new VBox(2, bulle, labelMeta);
        boite.setAlignment(cEstMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox ligne = new HBox();
        ligne.setPadding(new Insets(3, 16, 3, 16));
        ligne.setStyle("-fx-background-color: transparent;");

        if (cEstMoi) {
            ligne.setAlignment(Pos.CENTER_RIGHT);
            ligne.getChildren().add(boite);
        } else {
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.setSpacing(8);
            ligne.getChildren().addAll(construireAvatarCercle(expediteur, 14), boite);
        }

        listeVueMessages.getItems().add(ligne);
        listeVueMessages.scrollTo(listeVueMessages.getItems().size() - 1);
    }

    private StackPane construireAvatarCercle(String nom, double rayon) {
        String couleur = COULEURS_AVATAR[Math.abs(nom.hashCode()) % COULEURS_AVATAR.length];
        Circle cercle  = new Circle(rayon);
        cercle.setFill(Color.web(couleur));
        Label initiale = new Label(nom.substring(0, 1).toUpperCase());
        initiale.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
                + (int)(rayon * 0.85) + "px;");
        StackPane sp = new StackPane(cercle, initiale);
        sp.setMinWidth(rayon * 2);
        sp.setMinHeight(rayon * 2);
        return sp;
    }

    private void surErreur(String message) { afficherAlerte(message); }

    private void afficherAlerte(String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }

    @FXML
    private void gererDeconnexion() {
        connexion.sendLogout();
        MainApp.afficherConnexion();
    }
}