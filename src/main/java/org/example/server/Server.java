package org.example.server;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main server – listens on port 5000, spawns one thread per client (RG11).
 */
public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    public static final int PORT = 5000;

    // username -> handler (only one session per user, RG3)
    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Reset all users to OFFLINE on startup
        UserDAO userDAO = new UserDAO();
        userDAO.findAll().forEach(u -> userDAO.updateStatus(u.getId(), User.Status.OFFLINE));

        log.info("Serveur démarré sur le port {}", PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Nouvelle connexion depuis {}", clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, activeSessions);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            log.error("Erreur serveur : {}", e.getMessage());
        }
    }

    // ---------- helpers used by ClientHandler ----------

    public static void register(String username, ClientHandler handler) {
        activeSessions.put(username, handler);
    }

    public static void unregister(String username) {
        activeSessions.remove(username);
    }

    public static ClientHandler getHandler(String username) {
        return activeSessions.get(username);
    }

    public static boolean isOnline(String username) {
        return activeSessions.containsKey(username);
    }

    /** Broadcast updated user list to all connected clients. */
    public static void broadcastUserList() {
        activeSessions.values().forEach(ClientHandler::sendUserList);
    }
}
