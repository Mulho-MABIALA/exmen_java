//package org.example.server;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import org.example.dao.MessageDAO;
//import org.example.dao.UserDAO;
//import org.example.model.Message;
//import org.example.model.User;
//import org.example.util.PasswordUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.net.Socket;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Map;
//
///**
// * Handles one client connection in its own thread (RG11).
// */
//public class ClientHandler implements Runnable {
//
//    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
//    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//    private static final Gson GSON = new Gson();
//
//    private final Socket socket;
//    private final Map<String, ClientHandler> activeSessions;
//    private final UserDAO userDAO = new UserDAO();
//    private final MessageDAO messageDAO = new MessageDAO();
//
//    private PrintWriter out;
//    private String username;   // set after successful login
//
//    public ClientHandler(Socket socket, Map<String, ClientHandler> activeSessions) {
//        this.socket = socket;
//        this.activeSessions = activeSessions;
//    }
//
//    // ------------------------------------------------------------------ run
//    @Override
//    public void run() {
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
//
//            this.out = writer;
//            String line;
//            while ((line = in.readLine()) != null) {
//                handleRequest(line.trim());
//            }
//        } catch (IOException e) {
//            log.warn("Connexion perdue pour {} : {}", username, e.getMessage());
//        } finally {
//            disconnect();
//        }
//    }
//
//    // --------------------------------------------------------- dispatcher
//    private void handleRequest(String json) {
//        try {
//            JsonObject req = JsonParser.parseString(json).getAsJsonObject();
//            String type = req.get("type").getAsString();
//
//            switch (type) {
//                case "REGISTER"      -> handleRegister(req);
//                case "LOGIN"         -> handleLogin(req);
//                case "SEND_MESSAGE"  -> handleSendMessage(req);
//                case "GET_HISTORY"   -> handleGetHistory(req);
//                case "GET_USERS"     -> sendUserList();
//                case "LOGOUT"        -> disconnect();
//                default              -> sendError("Type de requête inconnu : " + type);
//            }
//        } catch (Exception e) {
//            sendError("Erreur serveur : " + e.getMessage());
//        }
//    }
//
//    // --------------------------------------------------------- handlers
//    private void handleRegister(JsonObject req) {
//        String uname = req.get("username").getAsString().trim();
//        String pwd   = req.get("password").getAsString();
//
//        if (uname.isEmpty() || pwd.isEmpty()) {
//            sendError("Username et mot de passe requis.");
//            return;
//        }
//        if (userDAO.existsByUsername(uname)) {   // RG1
//            send(error("REGISTER_FAIL", "Ce username est déjà utilisé."));
//            return;
//        }
//
//        User user = new User(uname, PasswordUtil.hash(pwd));  // RG9
//        userDAO.save(user);
//        log.info("Nouvel utilisateur inscrit : {}", uname);
//        send(ok("REGISTER_SUCCESS", "Inscription réussie."));
//    }
//
//    private void handleLogin(JsonObject req) {
//        String uname = req.get("username").getAsString().trim();
//        String pwd   = req.get("password").getAsString();
//
//        // RG3 – already connected?
//        if (activeSessions.containsKey(uname)) {
//            send(error("LOGIN_FAIL", "Cet utilisateur est déjà connecté."));
//            return;
//        }
//
//        User user = userDAO.findByUsername(uname).orElse(null);
//        if (user == null || !PasswordUtil.verify(pwd, user.getPassword())) {
//            send(error("LOGIN_FAIL", "Identifiants incorrects."));
//            return;
//        }
//
//        // RG4 – set ONLINE
//        this.username = uname;
//        userDAO.updateStatus(user.getId(), User.Status.ONLINE);
//        Server.register(uname, this);
//        log.info("Connexion : {}", uname);
//
//        JsonObject resp = new JsonObject();
//        resp.addProperty("type", "LOGIN_SUCCESS");
//        resp.addProperty("username", uname);
//        send(resp.toString());
//
//        // Deliver pending messages (RG6)
//        deliverPendingMessages(uname);
//
//        // Notify everyone
//        Server.broadcastUserList();
//    }
//
//    private void handleSendMessage(JsonObject req) {
//        if (!isAuthenticated()) return;
//
//        String receiver = req.get("receiver").getAsString().trim();
//        String content  = req.get("content").getAsString().trim();
//
//        // RG7 – validate content
//        if (content.isEmpty()) {
//            sendError("Le contenu du message ne peut pas être vide.");
//            return;
//        }
//        if (content.length() > 1000) {
//            sendError("Le message dépasse 1000 caractères.");
//            return;
//        }
//
//        // RG5 – receiver must exist
//        User senderUser   = userDAO.findByUsername(username).orElse(null);
//        User receiverUser = userDAO.findByUsername(receiver).orElse(null);
//        if (receiverUser == null) {
//            sendError("Destinataire introuvable : " + receiver);
//            return;
//        }
//
//        Message msg = new Message(senderUser, receiverUser, content);
//        messageDAO.save(msg);
//        log.info("Message de {} à {} : {}", username, receiver, content);  // RG12
//
//        // Build the wire payload
//        JsonObject payload = buildMessagePayload(username, content, msg.getDateEnvoi().format(FMT));
//
//        // Deliver immediately if online → statut RECU, sinon reste ENVOYE (RG6)
//        ClientHandler targetHandler = Server.getHandler(receiver);
//        boolean deliveredNow = targetHandler != null;
//        if (deliveredNow) {
//            targetHandler.send(payload.toString());
//            messageDAO.updateStatut(msg.getId(), Message.Statut.RECU);
//        }
//
//        // ✅ Confirmation à l'expéditeur avec statut ET id du message
//        JsonObject confirm = new JsonObject();
//        confirm.addProperty("type",      "MESSAGE_SENT");
//        confirm.addProperty("id",        msg.getId());                          // ← NOUVEAU
//        confirm.addProperty("receiver",  receiver);
//        confirm.addProperty("content",   content);
//        confirm.addProperty("dateEnvoi", msg.getDateEnvoi().format(FMT));
//        confirm.addProperty("statut",    deliveredNow ? "RECU" : "ENVOYE");    // ← NOUVEAU
//        send(confirm.toString());
//    }
//
//    private void handleGetHistory(JsonObject req) {
//        if (!isAuthenticated()) return;
//        String other = req.get("otherUser").getAsString().trim();
//
//        List<Message> messages = messageDAO.findConversation(username, other);
//
//        JsonObject resp = new JsonObject();
//        resp.addProperty("type", "HISTORY");
//        resp.addProperty("otherUser", other);
//
//        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
//        for (Message m : messages) {
//            JsonObject o = new JsonObject();
//            o.addProperty("id",       m.getId());                           // ← NOUVEAU
//            o.addProperty("sender",   m.getSender().getUsername());
//            o.addProperty("receiver", m.getReceiver().getUsername());
//            o.addProperty("content",  m.getContenu());
//            o.addProperty("dateEnvoi", m.getDateEnvoi().format(FMT));
//            o.addProperty("statut",   m.getStatut().name());
//            arr.add(o);
//        }
//        resp.add("messages", arr);
//        send(resp.toString());
//    }
//
//    // --------------------------------------------------------- helpers
//
//    /**
//     * Deliver messages sent while this user was offline (RG6).
//     * Notifie aussi l'expéditeur avec MESSAGE_DELIVERED pour mettre à jour ✓ → ✓✓.
//     */
//    private void deliverPendingMessages(String uname) {
//        List<Message> pending = messageDAO.findPendingMessages(uname);
//        for (Message m : pending) {
//            // 1. Livrer le message au destinataire (qui vient de se connecter)
//            send(buildMessagePayload(
//                    m.getSender().getUsername(),
//                    m.getContenu(),
//                    m.getDateEnvoi().format(FMT)).toString());
//
//            // 2. Mettre à jour le statut en DB
//            messageDAO.updateStatut(m.getId(), Message.Statut.RECU);
//
//            // 3. ✅ Notifier l'expéditeur : ✓ → ✓✓
//            ClientHandler senderHandler = Server.getHandler(m.getSender().getUsername());
//            if (senderHandler != null) {
//                JsonObject delivered = new JsonObject();
//                delivered.addProperty("type", "MESSAGE_DELIVERED");
//                delivered.addProperty("id",   m.getId());           // ← pour que le client mette à jour le bon message
//                senderHandler.send(delivered.toString());
//            }
//        }
//        if (!pending.isEmpty()) {
//            log.info("{} messages en attente livrés à {}", pending.size(), uname);
//        }
//    }
//
//    /** Send updated user list to THIS client. */
//    public void sendUserList() {
//        List<User> all = userDAO.findAll();
//        JsonObject resp = new JsonObject();
//        resp.addProperty("type", "USER_LIST");
//
//        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
//        for (User u : all) {
//            JsonObject o = new JsonObject();
//            o.addProperty("username", u.getUsername());
//            o.addProperty("status", u.getStatus().name());
//            arr.add(o);
//        }
//        resp.add("users", arr);
//        send(resp.toString());
//    }
//
//    private void disconnect() {
//        if (username != null) {
//            userDAO.findByUsername(username).ifPresent(u ->
//                    userDAO.updateStatus(u.getId(), User.Status.OFFLINE));  // RG4
//            Server.unregister(username);
//            log.info("Déconnexion : {}", username);  // RG12
//            username = null;
//            Server.broadcastUserList();
//        }
//        try { socket.close(); } catch (IOException ignored) {}
//    }
//
//    private boolean isAuthenticated() {
//        if (username == null) {
//            sendError("Non authentifié."); // RG2
//            return false;
//        }
//        return true;
//    }
//
//    public synchronized void send(String json) {
//        if (out != null) out.println(json);
//    }
//
//    private void sendError(String msg) { send(error("ERROR", msg)); }
//
//    private static String error(String type, String msg) {
//        JsonObject o = new JsonObject();
//        o.addProperty("type", type);
//        o.addProperty("message", msg);
//        return o.toString();
//    }
//
//    private static String ok(String type, String msg) {
//        JsonObject o = new JsonObject();
//        o.addProperty("type", type);
//        o.addProperty("message", msg);
//        return o.toString();
//    }
//
//    private static JsonObject buildMessagePayload(String sender, String content, String dateEnvoi) {
//        JsonObject o = new JsonObject();
//        o.addProperty("type", "MESSAGE");
//        o.addProperty("sender", sender);
//        o.addProperty("content", content);
//        o.addProperty("dateEnvoi", dateEnvoi);
//        return o;
//    }
//}
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
 * Gère une connexion client dans son propre thread (RG11).
 */
public class ClientHandler implements Runnable {

    private static final Logger journal   = LoggerFactory.getLogger(ClientHandler.class);
    private static final DateTimeFormatter FORMATEUR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new Gson();

    private final Socket socket;
    private final Map<String, ClientHandler> sessionsActives;
    private final UserDAO    daoUtilisateur = new UserDAO();
    private final MessageDAO daoMessage     = new MessageDAO();

    private PrintWriter sortie;
    private String nomUtilisateur; // défini après une connexion réussie

    public ClientHandler(Socket socket, Map<String, ClientHandler> sessionsActives) {
        this.socket          = socket;
        this.sessionsActives = sessionsActives;
    }

    // ------------------------------------------------------------------ run
    @Override
    public void run() {
        try (BufferedReader entree  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter    ecrivain = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            this.sortie = ecrivain;
            String ligne;
            while ((ligne = entree.readLine()) != null) {
                traiterRequete(ligne.trim());
            }
        } catch (IOException e) {
            journal.warn("Connexion perdue pour {} : {}", nomUtilisateur, e.getMessage());
        } finally {
            deconnecter();
        }
    }

    // --------------------------------------------------------- dispatcheur
    private void traiterRequete(String json) {
        try {
            JsonObject requete = JsonParser.parseString(json).getAsJsonObject();
            String type = requete.get("type").getAsString();

            switch (type) {
                case "REGISTER"     -> gererInscription(requete);
                case "LOGIN"        -> gererConnexion(requete);
                case "SEND_MESSAGE" -> gererEnvoiMessage(requete);
                case "GET_HISTORY"  -> gererHistorique(requete);
                case "GET_USERS"    -> envoyerListeUtilisateurs();
                case "LOGOUT"       -> deconnecter();
                default             -> envoyerErreur("Type de requête inconnu : " + type);
            }
        } catch (Exception e) {
            envoyerErreur("Erreur serveur : " + e.getMessage());
        }
    }

    // --------------------------------------------------------- gestionnaires
    private void gererInscription(JsonObject requete) {
        String nomU = requete.get("username").getAsString().trim();
        String mdp  = requete.get("password").getAsString();

        if (nomU.isEmpty() || mdp.isEmpty()) {
            envoyerErreur("Nom d'utilisateur et mot de passe requis.");
            return;
        }
        if (daoUtilisateur.existeParNomUtilisateur(nomU)) {  // RG1
            envoyer(erreur("REGISTER_FAIL", "Ce nom d'utilisateur est déjà utilisé."));
            return;
        }

        User utilisateur = new User(nomU, PasswordUtil.hash(mdp));  // RG9
        daoUtilisateur.sauvegarder(utilisateur);
        journal.info("Nouvel utilisateur inscrit : {}", nomU);
        envoyer(succes("REGISTER_SUCCESS", "Inscription réussie."));
    }

    private void gererConnexion(JsonObject requete) {
        String nomU = requete.get("username").getAsString().trim();
        String mdp  = requete.get("password").getAsString();

        // RG3 – déjà connecté ?
        if (sessionsActives.containsKey(nomU)) {
            envoyer(erreur("LOGIN_FAIL", "Cet utilisateur est déjà connecté."));
            return;
        }

        User utilisateur = daoUtilisateur.trouverParNomUtilisateur(nomU).orElse(null);
        if (utilisateur == null || !PasswordUtil.verify(mdp, utilisateur.getMotDePasse())) {
            envoyer(erreur("LOGIN_FAIL", "Identifiants incorrects."));
            return;
        }

        // RG4 – passer EN_LIGNE
        this.nomUtilisateur = nomU;
        daoUtilisateur.mettreAJourStatut(utilisateur.getId(), User.Statut.EN_LIGNE);
        Server.register(nomU, this);
        journal.info("Connexion : {}", nomU);

        JsonObject reponse = new JsonObject();
        reponse.addProperty("type", "LOGIN_SUCCESS");
        reponse.addProperty("username", nomU);
        envoyer(reponse.toString());

        // Livrer les messages en attente (RG6)
        livrerMessagesEnAttente(nomU);

        // Notifier tout le monde
        Server.broadcastUserList();
    }

    private void gererEnvoiMessage(JsonObject requete) {
        if (!estAuthentifie()) return;

        String destinataire = requete.get("receiver").getAsString().trim();
        String contenu      = requete.get("content").getAsString().trim();

        // RG7 – valider le contenu
        if (contenu.isEmpty()) {
            envoyerErreur("Le contenu du message ne peut pas être vide.");
            return;
        }
        if (contenu.length() > 1000) {
            envoyerErreur("Le message dépasse 1000 caractères.");
            return;
        }

        // RG5 – le destinataire doit exister
        User expediteur      = daoUtilisateur.trouverParNomUtilisateur(nomUtilisateur).orElse(null);
        User utilisateurDest = daoUtilisateur.trouverParNomUtilisateur(destinataire).orElse(null);
        if (utilisateurDest == null) {
            envoyerErreur("Destinataire introuvable : " + destinataire);
            return;
        }

        Message message = new Message(expediteur, utilisateurDest, contenu);
        daoMessage.sauvegarder(message);
        journal.info("Message de {} à {} : {}", nomUtilisateur, destinataire, contenu);  // RG12

        // Construire la charge utile réseau
        JsonObject charge = construireChargeMessage(nomUtilisateur, contenu, message.getDateEnvoi().format(FORMATEUR));

        // Livrer immédiatement si en ligne → statut RECU, sinon reste ENVOYE (RG6)
        ClientHandler gestionnaireDestinataire = Server.getHandler(destinataire);
        boolean livreMaintenant = gestionnaireDestinataire != null;
        if (livreMaintenant) {
            gestionnaireDestinataire.envoyer(charge.toString());
            daoMessage.mettreAJourStatut(message.getId(), Message.Statut.RECU);
        }

        // ✅ Confirmation à l'expéditeur avec statut ET id du message
        JsonObject confirmation = new JsonObject();
        confirmation.addProperty("type",      "MESSAGE_SENT");
        confirmation.addProperty("id",        message.getId());
        confirmation.addProperty("receiver",  destinataire);
        confirmation.addProperty("content",   contenu);
        confirmation.addProperty("dateEnvoi", message.getDateEnvoi().format(FORMATEUR));
        confirmation.addProperty("statut",    livreMaintenant ? "RECU" : "ENVOYE");
        envoyer(confirmation.toString());
    }

    private void gererHistorique(JsonObject requete) {
        if (!estAuthentifie()) return;
        String autreUtilisateur = requete.get("otherUser").getAsString().trim();

        List<Message> messages = daoMessage.trouverConversation(nomUtilisateur, autreUtilisateur);

        JsonObject reponse = new JsonObject();
        reponse.addProperty("type", "HISTORY");
        reponse.addProperty("otherUser", autreUtilisateur);

        com.google.gson.JsonArray tableau = new com.google.gson.JsonArray();
        for (Message m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("id",        m.getId());
            o.addProperty("sender",    m.getExpediteur().getNomUtilisateur());
            o.addProperty("receiver",  m.getDestinataire().getNomUtilisateur());
            o.addProperty("content",   m.getContenu());
            o.addProperty("dateEnvoi", m.getDateEnvoi().format(FORMATEUR));
            o.addProperty("statut",    m.getStatut().name());
            tableau.add(o);
        }
        reponse.add("messages", tableau);
        envoyer(reponse.toString());
    }

    // --------------------------------------------------------- utilitaires

    /**
     * Livre les messages envoyés pendant la déconnexion de l'utilisateur (RG6).
     * Notifie aussi l'expéditeur avec MESSAGE_DELIVERED pour mettre à jour ✓ → ✓✓.
     */
    private void livrerMessagesEnAttente(String nomU) {
        List<Message> enAttente = daoMessage.trouverMessagesEnAttente(nomU);
        for (Message m : enAttente) {
            // 1. Livrer le message au destinataire (qui vient de se connecter)
            envoyer(construireChargeMessage(
                    m.getExpediteur().getNomUtilisateur(),
                    m.getContenu(),
                    m.getDateEnvoi().format(FORMATEUR)).toString());

            // 2. Mettre à jour le statut en base de données
            daoMessage.mettreAJourStatut(m.getId(), Message.Statut.RECU);

            // 3. ✅ Notifier l'expéditeur : ✓ → ✓✓
            ClientHandler gestionnaireExpediteur = Server.getHandler(m.getExpediteur().getNomUtilisateur());
            if (gestionnaireExpediteur != null) {
                JsonObject livre = new JsonObject();
                livre.addProperty("type", "MESSAGE_DELIVERED");
                livre.addProperty("id",   m.getId());
                gestionnaireExpediteur.envoyer(livre.toString());
            }
        }
        if (!enAttente.isEmpty()) {
            journal.info("{} messages en attente livrés à {}", enAttente.size(), nomU);
        }
    }

    /** Envoie la liste des utilisateurs à jour à CE client. */
//    public void envoyerListeUtilisateurs() {
//        List<User> tous = daoUtilisateur.trouverTous();
//        JsonObject reponse = new JsonObject();
//        reponse.addProperty("type", "USER_LIST");
//
//        com.google.gson.JsonArray tableau = new com.google.gson.JsonArray();
//        for (User u : tous) {
//            JsonObject o = new JsonObject();
//            o.addProperty("username", u.getNomUtilisateur());
//            o.addProperty("status",   u.getStatut().name());
//            tableau.add(o);
//        }
//        reponse.add("users", tableau);
//        envoyer(reponse.toString());
//    }
    public void envoyerListeUtilisateurs() {
        List<User> tous = daoUtilisateur.trouverTous();
        JsonObject reponse = new JsonObject();
        reponse.addProperty("type", "USER_LIST");

        com.google.gson.JsonArray tableau = new com.google.gson.JsonArray();
        for (User u : tous) {
            JsonObject o = new JsonObject();
            o.addProperty("username", u.getNomUtilisateur());

            // ✅ Vérifier la session active plutôt que la BDD
            boolean estConnecte = Server.getHandler(u.getNomUtilisateur()) != null;
            o.addProperty("status", estConnecte ? "EN_LIGNE" : "HORS_LIGNE");

            tableau.add(o);
        }
        reponse.add("users", tableau);
        envoyer(reponse.toString());
    }

    private void deconnecter() {
        if (nomUtilisateur != null) {
            daoUtilisateur.trouverParNomUtilisateur(nomUtilisateur).ifPresent(u ->
                    daoUtilisateur.mettreAJourStatut(u.getId(), User.Statut.HORS_LIGNE));  // RG4
            Server.unregister(nomUtilisateur);
            journal.info("Déconnexion : {}", nomUtilisateur);  // RG12
            nomUtilisateur = null;
            Server.broadcastUserList();
        }
        try { socket.close(); } catch (IOException ignore) {}
    }

    private boolean estAuthentifie() {
        if (nomUtilisateur == null) {
            envoyerErreur("Non authentifié."); // RG2
            return false;
        }
        return true;
    }

    public synchronized void envoyer(String json) {
        if (sortie != null) sortie.println(json);
    }

    private void envoyerErreur(String message) { envoyer(erreur("ERROR", message)); }

    private static String erreur(String type, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("message", message);
        return o.toString();
    }

    private static String succes(String type, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("message", message);
        return o.toString();
    }

    private static JsonObject construireChargeMessage(String expediteur, String contenu, String dateEnvoi) {
        JsonObject o = new JsonObject();
        o.addProperty("type",      "MESSAGE");
        o.addProperty("sender",    expediteur);
        o.addProperty("content",   contenu);
        o.addProperty("dateEnvoi", dateEnvoi);
        return o;
    }
}