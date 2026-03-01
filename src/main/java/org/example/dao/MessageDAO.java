package org.example.dao;

import org.example.model.Message;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MessageDAO {

    public Message save(Message message) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(message);
            tx.commit();
            return message;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Returns conversation between two users ordered chronologically (RG8).
     */
    public List<Message> findConversation(String user1, String user2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Message m WHERE " +
                    "(m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", user1)
                    .setParameter("u2", user2)
                    .list();
        }
    }

    /**
     * Returns pending messages for a user (sent while offline).
     */
    public List<Message> findPendingMessages(String receiverUsername) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Message m WHERE m.receiver.username = :receiver " +
                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("receiver", receiverUsername)
                    .setParameter("statut", Message.Statut.ENVOYE)
                    .list();
        }
    }

    public void updateStatut(Long messageId, Message.Statut statut) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", messageId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
