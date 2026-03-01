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
 * Manages the TCP connection to the server.
 * All incoming messages are dispatched to a listener on the JavaFX thread.
 */
public class ServerConnection {

    private static final Logger log = LoggerFactory.getLogger(ServerConnection.class);

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private Consumer<JsonObject> messageListener;
    private Consumer<String>     errorListener;

    public ServerConnection(Consumer<JsonObject> messageListener, Consumer<String> errorListener) {
        this.messageListener = messageListener;
        this.errorListener   = errorListener;
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Listen for incoming messages in a daemon thread
            Thread reader = new Thread(this::readLoop);
            reader.setDaemon(true);
            reader.start();
            return true;
        } catch (IOException e) {
            log.error("Impossible de se connecter au serveur : {}", e.getMessage());
            return false;
        }
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                final JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                Platform.runLater(() -> messageListener.accept(json));
            }
        } catch (IOException e) {
            log.warn("Connexion au serveur perdue : {}", e.getMessage());
            // RG10
            Platform.runLater(() -> errorListener.accept(
                    "Connexion au serveur perdue. Vous êtes hors ligne."));
        }
    }

    public void send(JsonObject obj) {
        if (out != null) out.println(obj.toString());
    }

    /** Allow ChatController to replace the listeners after login. */
    public void setMessageListener(Consumer<JsonObject> listener) {
        this.messageListener = listener;
    }

    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = listener;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // -------------------- convenience builders --------------------

    public void sendLogin(String username, String password) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "LOGIN");
        req.addProperty("username", username);
        req.addProperty("password", password);
        send(req);
    }

    public void sendRegister(String username, String password) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "REGISTER");
        req.addProperty("username", username);
        req.addProperty("password", password);
        send(req);
    }

    public void sendMessage(String receiver, String content) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "SEND_MESSAGE");
        req.addProperty("receiver", receiver);
        req.addProperty("content", content);
        send(req);
    }

    public void requestHistory(String otherUser) {
        JsonObject req = new JsonObject();
        req.addProperty("type", "GET_HISTORY");
        req.addProperty("otherUser", otherUser);
        send(req);
    }

    public void requestUserList() {
        JsonObject req = new JsonObject();
        req.addProperty("type", "GET_USERS");
        send(req);
    }

    public void sendLogout() {
        JsonObject req = new JsonObject();
        req.addProperty("type", "LOGOUT");
        send(req);
        disconnect();
    }
}
