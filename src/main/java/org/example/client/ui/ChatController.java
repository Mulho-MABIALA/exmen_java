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

public class ChatController {

    @FXML private Label          currentUserLabel;
    @FXML private Label          chatWithLabel;
    @FXML private Label          chatStatusLabel;
    @FXML private StackPane      contactAvatar;
    @FXML private ListView<HBox> messageListView;
    @FXML private ListView<HBox> userListView;
    @FXML private TextField      messageField;

    private String           currentUser;
    private String           selectedUser;
    private ServerConnection connection;

    private static final String[] AVATAR_COLORS = {
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#009688", "#4CAF50",
        "#FF9800", "#FF5722", "#795548", "#607D8B"
    };

    public void init(String username, ServerConnection conn) {
        this.currentUser = username;
        this.connection  = conn;
        currentUserLabel.setText(username);
        conn.setMessageListener(this::onMessage);
        conn.setErrorListener(this::onError);

        userListView.setOnMouseClicked(e -> {
            HBox sel = userListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String uname = (String) sel.getUserData();
                if (uname != null && !uname.equals(currentUser)) openConversation(uname);
            }
        });

        messageField.setOnAction(e -> handleSend());
        conn.requestUserList();
    }

    private void openConversation(String uname) {
        selectedUser = uname;
        chatWithLabel.setText(uname);
        chatStatusLabel.setText("chargement...");
        messageListView.getItems().clear();
        contactAvatar.getChildren().setAll(buildAvatarCircle(uname, 20));
        connection.requestHistory(uname);
    }

    @FXML
    private void handleSend() {
        if (selectedUser == null) { showAlert("Selectionnez un utilisateur."); return; }
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (text.length() > 1000) { showAlert("Max 1000 caracteres."); return; }
        connection.sendMessage(selectedUser, text);
        messageField.clear();
    }

    private void onMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "USER_LIST"    -> handleUserList(json);
            case "MESSAGE"      -> handleIncomingMessage(json);
            case "MESSAGE_SENT" -> handleSentConfirmation(json);
            case "HISTORY"      -> handleHistory(json);
            case "ERROR"        -> showAlert(json.get("message").getAsString());
        }
    }

    private void handleUserList(JsonObject json) {
        userListView.getItems().clear();
        JsonArray users = json.getAsJsonArray("users");
        for (int i = 0; i < users.size(); i++) {
            JsonObject u     = users.get(i).getAsJsonObject();
            String uname     = u.get("username").getAsString();
            boolean isOnline = "ONLINE".equals(u.get("status").getAsString());
            boolean isMe     = uname.equals(currentUser);
            boolean isSel    = uname.equals(selectedUser);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 14));
            row.setUserData(uname);
            if (isSel) {
                row.setStyle("-fx-background-color: #E8F5E9; -fx-cursor: hand;"
                    + " -fx-border-color: #00A884 transparent transparent transparent;"
                    + " -fx-border-width: 0 0 0 3;");
            } else {
                row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            }

            VBox info = new VBox(2);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            Label nameLabel = new Label(uname + (isMe ? "  (moi)" : ""));
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111B21;"
                    + (isMe ? " -fx-font-style: italic;" : ""));

            Label statusLabel = new Label(isOnline ? "En ligne" : "Hors ligne");
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                    + (isOnline ? "#00A884;" : "#8696A0;"));

            info.getChildren().addAll(nameLabel, statusLabel);

            Circle dot = new Circle(4);
            dot.setFill(Color.web(isOnline ? "#25D366" : "#B0BEC5"));

            row.getChildren().addAll(buildAvatarCircle(uname, 20), info, dot);
            userListView.getItems().add(row);

            if (isSel) chatStatusLabel.setText(isOnline ? "En ligne" : "Hors ligne");
        }
    }

    private void handleIncomingMessage(JsonObject json) {
        String sender = json.get("sender").getAsString();
        if (sender.equals(selectedUser))
            appendMessage(sender, json.get("content").getAsString(),
                json.get("dateEnvoi").getAsString(), false);
    }

    private void handleSentConfirmation(JsonObject json) {
        appendMessage(currentUser, json.get("content").getAsString(),
            json.get("dateEnvoi").getAsString(), true);
    }

    private void handleHistory(JsonObject json) {
        messageListView.getItems().clear();
        chatStatusLabel.setText("");
        JsonArray messages = json.getAsJsonArray("messages");
        for (int i = 0; i < messages.size(); i++) {
            JsonObject m = messages.get(i).getAsJsonObject();
            appendMessage(m.get("sender").getAsString(),
                m.get("content").getAsString(), m.get("dateEnvoi").getAsString(),
                m.get("sender").getAsString().equals(currentUser));
        }
    }

    private void appendMessage(String sender, String content, String date, boolean isMine) {
        Label bubble = new Label(content);
        bubble.setWrapText(true);
        bubble.setMaxWidth(460);
        bubble.setPadding(new Insets(9, 14, 9, 14));
        if (isMine) {
            bubble.setStyle("-fx-background-color: #D9FDD3;"
                + " -fx-background-radius: 16 2 16 16; -fx-font-size: 13.5px;"
                + " -fx-text-fill: #111B21;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        } else {
            bubble.setStyle("-fx-background-color: white;"
                + " -fx-background-radius: 2 16 16 16; -fx-font-size: 13.5px;"
                + " -fx-text-fill: #111B21;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");
        }

        String timeOnly = date.length() >= 16 ? date.substring(11, 16) : date;
        Label meta = new Label(timeOnly + (isMine ? "  \u2713\u2713" : ""));
        meta.setStyle("-fx-font-size: 10px; -fx-text-fill: #8696A0; -fx-padding: 1 4 0 4;");

        VBox box = new VBox(2, bubble, meta);
        box.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox();
        row.setPadding(new Insets(3, 16, 3, 16));
        row.setStyle("-fx-background-color: transparent;");

        if (isMine) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(box);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(8);
            row.getChildren().addAll(buildAvatarCircle(sender, 14), box);
        }

        messageListView.getItems().add(row);
        messageListView.scrollTo(messageListView.getItems().size() - 1);
    }

    private StackPane buildAvatarCircle(String name, double radius) {
        String color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
        Circle circle = new Circle(radius);
        circle.setFill(Color.web(color));
        Label initial = new Label(name.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
            + (int)(radius * 0.85) + "px;");
        StackPane sp = new StackPane(circle, initial);
        sp.setMinWidth(radius * 2);
        sp.setMinHeight(radius * 2);
        return sp;
    }

    private void onError(String msg) { showAlert(msg); }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        connection.sendLogout();
        MainApp.showLogin();
    }
}
