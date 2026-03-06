//package org.example.server;
//
//import org.example.dao.UserDAO;
//import org.example.model.User;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//
//public class Server {
//
//    private static final Logger log = LoggerFactory.getLogger(Server.class);
//    public static final int PORT = 5000;
//
//    // username -> handler (only one session per user, RG3)
//    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();
//
//    public static void main(String[] args) {
//        // Reset all users to OFFLINE on startup
//        UserDAO userDAO = new UserDAO();
//        userDAO.findAll().forEach(u -> userDAO.updateStatus(u.getId(), User.Status.OFFLINE));
//
//        log.info("Serveur démarré sur le port {}", PORT);
//
//        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                log.info("Nouvelle connexion depuis {}", clientSocket.getInetAddress());
//                ClientHandler handler = new ClientHandler(clientSocket, activeSessions);
//                Thread thread = new Thread(handler);
//                thread.setDaemon(true);
//                thread.start();
//            }
//        } catch (IOException e) {
//            log.error("Erreur serveur : {}", e.getMessage());
//        }
//    }
//
//    // ---------- helpers used by ClientHandler ----------
//
//    public static void register(String username, ClientHandler handler) {
//        activeSessions.put(username, handler);
//    }
//
//    public static void unregister(String username) {
//        activeSessions.remove(username);
//    }
//
//    public static ClientHandler getHandler(String username) {
//        return activeSessions.get(username);
//    }
//
//    public static boolean isOnline(String username) {
//        return activeSessions.containsKey(username);
//    }
//
//    /** Broadcast updated user list to all connected clients. */
//    public static void broadcastUserList() {
//        activeSessions.values().forEach(ClientHandler::sendUserList);
//    }
//}

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


public class Server {

    private static final Logger journal = LoggerFactory.getLogger(Server.class);
    public static final int PORT = 5000;

    // nomUtilisateur -> gestionnaire (une seule session par utilisateur, RG3)
    private static final Map<String, ClientHandler> sessionsActives = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Remettre tous les utilisateurs à HORS_LIGNE au démarrage
        UserDAO daoUtilisateur = new UserDAO();
        daoUtilisateur.trouverTous().forEach(u ->
                daoUtilisateur.mettreAJourStatut(u.getId(), User.Statut.HORS_LIGNE));

        journal.info("Serveur démarré sur le port {}", PORT);

        try (ServerSocket socketServeur = new ServerSocket(PORT)) {
            while (true) {
                Socket socketClient = socketServeur.accept();
                journal.info("Nouvelle connexion depuis {}", socketClient.getInetAddress());
                ClientHandler gestionnaire = new ClientHandler(socketClient, sessionsActives);
                Thread thread = new Thread(gestionnaire);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            journal.error("Erreur serveur : {}", e.getMessage());
        }
    }

    // ---------- utilitaires utilisés par ClientHandler ----------

    public static void register(String nomUtilisateur, ClientHandler gestionnaire) {
        sessionsActives.put(nomUtilisateur, gestionnaire);
    }

    public static void unregister(String nomUtilisateur) {
        sessionsActives.remove(nomUtilisateur);
    }

    public static ClientHandler getHandler(String nomUtilisateur) {
        return sessionsActives.get(nomUtilisateur);
    }

    public static boolean estEnLigne(String nomUtilisateur) {
        return sessionsActives.containsKey(nomUtilisateur);
    }

    /** Diffuse la liste des utilisateurs mise à jour à tous les clients connectés. */
    public static void broadcastUserList() {
        sessionsActives.values().forEach(ClientHandler::envoyerListeUtilisateurs);
    }
}