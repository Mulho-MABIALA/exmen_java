////package org.example.dao;
////
////import org.example.model.Message;
////import org.example.util.HibernateUtil;
////import org.hibernate.Session;
////import org.hibernate.Transaction;
////
////import java.util.List;
////
////public class MessageDAO {
////
////    public Message save(Message message) {
////        Transaction tx = null;
////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
////            tx = session.beginTransaction();
////            session.persist(message);
////            tx.commit();
////            return message;
////        } catch (Exception e) {
////            if (tx != null) tx.rollback();
////            throw e;
////        }
////    }
////
////    /**
////     * Returns conversation between two users ordered chronologically (RG8).
////     */
//////    public List<Message> findConversation(String user1, String user2) {
//////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//////            return session.createQuery(
//////                    "FROM Message m WHERE " +
//////                    "(m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
//////                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
//////                    "ORDER BY m.dateEnvoi ASC", Message.class)
//////                    .setParameter("u1", user1)
//////                    .setParameter("u2", user2)
//////                    .list();
//////        }
//////    }
////    public List<Message> findConversation(String user1, String user2) {
////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
////            return session.createQuery(
////                            "FROM Message m " +
////                                    "JOIN FETCH m.sender " +   // ← forcer le chargement
////                                    "JOIN FETCH m.receiver " + // ← forcer le chargement
////                                    "WHERE (m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
////                                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
////                                    "ORDER BY m.dateEnvoi ASC", Message.class)
////                    .setParameter("u1", user1)
////                    .setParameter("u2", user2)
////                    .list();
////        }
////    }
////
////    /**
////     * Returns pending messages for a user (sent while offline).
////     */
//////    public List<Message> findPendingMessages(String receiverUsername) {
//////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//////            return session.createQuery(
//////                    "FROM Message m WHERE m.receiver.username = :receiver " +
//////                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
//////                    .setParameter("receiver", receiverUsername)
//////                    .setParameter("statut", Message.Statut.ENVOYE)
//////                    .list();
//////        }
//////    }
////    public List<Message> findPendingMessages(String receiverUsername) {
////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
////            return session.createQuery(
////                            "FROM Message m " +
////                                    "JOIN FETCH m.sender " +   // ← forcer le chargement
////                                    "JOIN FETCH m.receiver " + // ← forcer le chargement
////                                    "WHERE m.receiver.username = :receiver " +
////                                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
////                    .setParameter("receiver", receiverUsername)
////                    .setParameter("statut", Message.Statut.ENVOYE)
////                    .list();
////        }
////    }
////
////    public void updateStatut(Long messageId, Message.Statut statut) {
////        Transaction tx = null;
////        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
////            tx = session.beginTransaction();
////            session.createMutationQuery(
////                    "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
////                    .setParameter("statut", statut)
////                    .setParameter("id", messageId)
////                    .executeUpdate();
////            tx.commit();
////        } catch (Exception e) {
////            if (tx != null) tx.rollback();
////            throw e;
////        }
////    }
////}
//
//package org.example.dao;
//
//import org.example.model.Message;
//import org.example.util.HibernateUtil;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//
//import java.util.List;
//
//public class MessageDAO {
//
//    public Message sauvegarder(Message message) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.persist(message);
//            transaction.commit();
//            return message;
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            throw e;
//        }
//    }
//
//    /**
//     * Retourne la conversation entre deux utilisateurs dans l'ordre chronologique (RG8).
//     */
//    public List<Message> trouverConversation(String utilisateur1, String utilisateur2) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery(
//                            "FROM Message m " +
//                                    "JOIN FETCH m.sender " +   // ← forcer le chargement
//                                    "JOIN FETCH m.receiver " + // ← forcer le chargement
//                                    "WHERE (m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
//                                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
//                                    "ORDER BY m.dateEnvoi ASC", Message.class)
//                    .setParameter("u1", utilisateur1)
//                    .setParameter("u2", utilisateur2)
//                    .list();
//        }
//    }
//
//    /**
//     * Retourne les messages en attente pour un utilisateur (envoyés pendant sa déconnexion).
//     */
//    public List<Message> trouverMessagesEnAttente(String nomDestinataire) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery(
//                            "FROM Message m " +
//                                    "JOIN FETCH m.sender " +   // ← forcer le chargement
//                                    "JOIN FETCH m.receiver " + // ← forcer le chargement
//                                    "WHERE m.receiver.username = :destinataire " +
//                                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
//                    .setParameter("destinataire", nomDestinataire)
//                    .setParameter("statut", Message.Statut.ENVOYE)
//                    .list();
//        }
//    }
//
//    public void mettreAJourStatut(Long idMessage, Message.Statut statut) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.createMutationQuery(
//                            "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
//                    .setParameter("statut", statut)
//                    .setParameter("id", idMessage)
//                    .executeUpdate();
//            transaction.commit();
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            throw e;
//        }
//    }
//}

package org.example.dao;

import org.example.model.Message;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MessageDAO {

    public Message sauvegarder(Message message) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(message);
            transaction.commit();
            return message;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    /**
     * Retourne la conversation entre deux utilisateurs dans l'ordre chronologique (RG8).
     */
    public List<Message> trouverConversation(String utilisateur1, String utilisateur2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m " +
                                    "JOIN FETCH m.expediteur " +   // ← forcer le chargement
                                    "JOIN FETCH m.destinataire " + // ← forcer le chargement
                                    "WHERE (m.expediteur.nomUtilisateur = :u1 AND m.destinataire.nomUtilisateur = :u2) OR " +
                                    "(m.expediteur.nomUtilisateur = :u2 AND m.destinataire.nomUtilisateur = :u1) " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", utilisateur1)
                    .setParameter("u2", utilisateur2)
                    .list();
        }
    }

    /**
     * Retourne les messages en attente pour un utilisateur (envoyés pendant sa déconnexion).
     */
    public List<Message> trouverMessagesEnAttente(String nomDestinataire) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m " +
                                    "JOIN FETCH m.expediteur " +   // ← forcer le chargement
                                    "JOIN FETCH m.destinataire " + // ← forcer le chargement
                                    "WHERE m.destinataire.nomUtilisateur = :destinataire " +
                                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("destinataire", nomDestinataire)
                    .setParameter("statut", Message.Statut.ENVOYE)
                    .list();
        }
    }

    public void mettreAJourStatut(Long idMessage, Message.Statut statut) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createMutationQuery(
                            "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", idMessage)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }
}
