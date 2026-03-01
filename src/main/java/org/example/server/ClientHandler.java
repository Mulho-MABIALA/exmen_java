package org.example.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.dao.MessageDAO;
import org.example.dao.UserDAO;
import org.example.model.Message;
import org.example.model.User;
import org.example.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Handles one client connection in its own thread (RG11).
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new Gson();

    private final Socket socket;
    private final Map<String, ClientHandler> activeSessions;
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    private PrintWriter out;
    private String username;   // set after successful login

    public ClientHandler(Socket socket, Map<String, ClientHandler> activeSessions) {
        this.socket = socket;
        this.activeSessions = activeSessions;
    }

    // ------------------------------------------------------------------ run
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            this.out = writer;
            String line;
            while ((line = in.readLine()) != null) {
                handleRequest(line.trim());
            }
        } catch (IOException e) {
            log.warn("Connexion perdue pour {} : {}", username, e.getMessage());
        } finally {
            disconnect();
        }
    }

    // --------------------------------------------------------- dispatcher
    private void handleRequest(String json) {
        try {
            JsonObject req = JsonParser.parseString(json).getAsJsonObject();
            String type = req.get("type").getAsString();

            switch (type) {
                case "REGISTER"      -> handleRegister(req);
                case "LOGIN"         -> handleLogin(req);
                case "SEND_MESSAGE"  -> handleSendMessage(req);
                case "GET_HISTORY"   -> handleGetHistory(req);
                case "GET_USERS"     -> sendUserList();
                case "LOGOUT"        -> disconnect();
                default              -> sendError("Type de requête inconnu : " + type);
            }
        } catch (Exception e) {
            sendError("Erreur serveur : " + e.getMessage());
        }
    }

    // --------------------------------------------------------- handlers
    private void handleRegister(JsonObject req) {
        String uname = req.get("username").getAsString().trim();
        String pwd   = req.get("password").getAsString();

        if (uname.isEmpty() || pwd.isEmpty()) {
            sendError("Username et mot de passe requis.");
            return;
        }
        if (userDAO.existsByUsername(uname)) {   // RG1
            send(error("REGISTER_FAIL", "Ce username est déjà utilisé."));
            return;
        }

        User user = new User(uname, PasswordUtil.hash(pwd));  // RG9
        userDAO.save(user);
        log.info("Nouvel utilisateur inscrit : {}", uname);
        send(ok("REGISTER_SUCCESS", "Inscription réussie."));
    }

    private void handleLogin(JsonObject req) {
        String uname = req.get("username").getAsString().trim();
        String pwd   = req.get("password").getAsString();

        // RG3 – already connected?
        if (activeSessions.containsKey(uname)) {
            send(error("LOGIN_FAIL", "Cet utilisateur est déjà connecté."));
            return;
        }

        User user = userDAO.findByUsername(uname).orElse(null);
        if (user == null || !PasswordUtil.verify(pwd, user.getPassword())) {
            send(error("LOGIN_FAIL", "Identifiants incorrects."));
            return;
        }

        // RG4 – set ONLINE
        this.username = uname;
        userDAO.updateStatus(user.getId(), User.Status.ONLINE);
        Server.register(uname, this);
        log.info("Connexion : {}", uname);

        JsonObject resp = new JsonObject();
        resp.addProperty("type", "LOGIN_SUCCESS");
        resp.addProperty("username", uname);
        send(resp.toString());

        // Deliver pending messages (RG6)
        deliverPendingMessages(uname);

        // Notify everyone
        Server.broadcastUserList();
    }

    private void handleSendMessage(JsonObject req) {
        if (!isAuthenticated()) return;

        String receiver = req.get("receiver").getAsString().trim();
        String content  = req.get("content").getAsString().trim();

        // RG7 – validate content
        if (content.isEmpty()) {
            sendError("Le contenu du message ne peut pas être vide.");
            return;
        }
        if (content.length() > 1000) {
            sendError("Le message dépasse 1000 caractères.");
            return;
        }

        // RG5 – receiver must exist
        User senderUser   = userDAO.findByUsername(username).orElse(null);
        User receiverUser = userDAO.findByUsername(receiver).orElse(null);
        if (receiverUser == null) {
            sendError("Destinataire introuvable : " + receiver);
            return;
        }

        Message msg = new Message(senderUser, receiverUser, content);
        messageDAO.save(msg);
        log.info("Message de {} à {} : {}", username, receiver, content);  // RG12

        // Build the wire payload
        JsonObject payload = buildMessagePayload(username, content, msg.getDateEnvoi().format(FMT));

        // Deliver immediately if online, otherwise keep status ENVOYE (RG6)
        ClientHandler targetHandler = Server.getHandler(receiver);
        if (targetHandler != null) {
            targetHandler.send(payload.toString());
            messageDAO.updateStatut(msg.getId(), Message.Statut.RECU);
        }

        // Confirm to sender
        JsonObject confirm = new JsonObject();
        confirm.addProperty("type", "MESSAGE_SENT");
        confirm.addProperty("receiver", receiver);
        confirm.addProperty("content", content);
        confirm.addProperty("dateEnvoi", msg.getDateEnvoi().format(FMT));
        send(confirm.toString());
    }

    private void handleGetHistory(JsonObject req) {
        if (!isAuthenticated()) return;
        String other = req.get("otherUser").getAsString().trim();

        List<Message> messages = messageDAO.findConversation(username, other);

        JsonObject resp = new JsonObject();
        resp.addProperty("type", "HISTORY");
        resp.addProperty("otherUser", other);

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (Message m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("sender",   m.getSender().getUsername());
            o.addProperty("receiver", m.getReceiver().getUsername());
            o.addProperty("content",  m.getContenu());
            o.addProperty("dateEnvoi", m.getDateEnvoi().format(FMT));
            o.addProperty("statut",   m.getStatut().name());
            arr.add(o);
        }
        resp.add("messages", arr);
        send(resp.toString());
    }

    // --------------------------------------------------------- helpers
    /** Deliver messages sent while this user was offline (RG6). */
    private void deliverPendingMessages(String uname) {
        List<Message> pending = messageDAO.findPendingMessages(uname);
        for (Message m : pending) {
            send(buildMessagePayload(m.getSender().getUsername(),
                    m.getContenu(), m.getDateEnvoi().format(FMT)).toString());
            messageDAO.updateStatut(m.getId(), Message.Statut.RECU);
        }
        if (!pending.isEmpty()) {
            log.info("{} messages en attente livrés à {}", pending.size(), uname);
        }
    }

    /** Send updated user list to THIS client. */
    public void sendUserList() {
        List<User> all = userDAO.findAll();
        JsonObject resp = new JsonObject();
        resp.addProperty("type", "USER_LIST");

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (User u : all) {
            JsonObject o = new JsonObject();
            o.addProperty("username", u.getUsername());
            o.addProperty("status", u.getStatus().name());
            arr.add(o);
        }
        resp.add("users", arr);
        send(resp.toString());
    }

    private void disconnect() {
        if (username != null) {
            userDAO.findByUsername(username).ifPresent(u ->
                    userDAO.updateStatus(u.getId(), User.Status.OFFLINE));  // RG4
            Server.unregister(username);
            log.info("Déconnexion : {}", username);  // RG12
            username = null;
            Server.broadcastUserList();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    private boolean isAuthenticated() {
        if (username == null) {
            sendError("Non authentifié."); // RG2
            return false;
        }
        return true;
    }

    public synchronized void send(String json) {
        if (out != null) out.println(json);
    }

    private void sendError(String msg) { send(error("ERROR", msg)); }

    private static String error(String type, String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("message", msg);
        return o.toString();
    }

    private static String ok(String type, String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("message", msg);
        return o.toString();
    }

    private static JsonObject buildMessagePayload(String sender, String content, String dateEnvoi) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "MESSAGE");
        o.addProperty("sender", sender);
        o.addProperty("content", content);
        o.addProperty("dateEnvoi", dateEnvoi);
        return o;
    }
}
