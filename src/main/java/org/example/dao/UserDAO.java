//package org.example.dao;
//
//import org.example.model.User;
//import org.example.util.HibernateUtil;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//
//import java.util.List;
//import java.util.Optional;
//
//public class UserDAO {
//
//    public Optional<User> findByUsername(String username) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            User user = session.createQuery(
//                    "FROM User u WHERE u.username = :username", User.class)
//                    .setParameter("username", username)
//                    .uniqueResult();
//            return Optional.ofNullable(user);
//        }
//    }
//
//    public boolean existsByUsername(String username) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            Long count = session.createQuery(
//                    "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
//                    .setParameter("username", username)
//                    .uniqueResult();
//            return count != null && count > 0;
//        }
//    }
//
//    public User save(User user) {
//        Transaction tx = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            tx = session.beginTransaction();
//            session.persist(user);
//            tx.commit();
//            return user;
//        } catch (Exception e) {
//            if (tx != null) tx.rollback();
//            throw e;
//        }
//    }
//
//    public void updateStatus(Long userId, User.Status status) {
//        Transaction tx = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            tx = session.beginTransaction();
//            session.createMutationQuery(
//                    "UPDATE User u SET u.status = :status WHERE u.id = :id")
//                    .setParameter("status", status)
//                    .setParameter("id", userId)
//                    .executeUpdate();
//            tx.commit();
//        } catch (Exception e) {
//            if (tx != null) tx.rollback();
//            throw e;
//        }
//    }
//
//    public List<User> findAll() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery("FROM User u ORDER BY u.username", User.class).list();
//        }
//    }
//
//    public List<User> findOnlineUsers() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery(
//                    "FROM User u WHERE u.status = :status ORDER BY u.username", User.class)
//                    .setParameter("status", User.Status.ONLINE)
//                    .list();
//        }
//    }
//}

//package org.example.dao;
//
//import org.example.model.User;
//import org.example.util.HibernateUtil;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//
//import java.util.List;
//import java.util.Optional;
//
//public class UserDAO {
//
//    public Optional<User> trouverParNomUtilisateur(String nomUtilisateur) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            User utilisateur = session.createQuery(
//                            "FROM User u WHERE u.username = :nomUtilisateur", User.class)
//                    .setParameter("nomUtilisateur", nomUtilisateur)
//                    .uniqueResult();
//            return Optional.ofNullable(utilisateur);
//        }
//    }
//
//    public boolean existeParNomUtilisateur(String nomUtilisateur) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            Long nombre = session.createQuery(
//                            "SELECT COUNT(u) FROM User u WHERE u.username = :nomUtilisateur", Long.class)
//                    .setParameter("nomUtilisateur", nomUtilisateur)
//                    .uniqueResult();
//            return nombre != null && nombre > 0;
//        }
//    }
//
//    public User sauvegarder(User utilisateur) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.persist(utilisateur);
//            transaction.commit();
//            return utilisateur;
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            throw e;
//        }
//    }
//
//    public void mettreAJourStatut(Long idUtilisateur, User.Status statut) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.createMutationQuery(
//                            "UPDATE User u SET u.status = :statut WHERE u.id = :id")
//                    .setParameter("statut", statut)
//                    .setParameter("id", idUtilisateur)
//                    .executeUpdate();
//            transaction.commit();
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            throw e;
//        }
//    }
//
//    public List<User> trouverTous() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery(
//                    "FROM User u ORDER BY u.username", User.class).list();
//        }
//    }
//
//    public List<User> trouverUtilisateursEnLigne() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery(
//                            "FROM User u WHERE u.status = :statut ORDER BY u.username", User.class)
//                    .setParameter("statut", User.Status.EN_LIGNE)
//                    .list();
//        }
//    }
//}

package org.example.dao;

import org.example.model.User;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class UserDAO {

    public Optional<User> trouverParNomUtilisateur(String nomUtilisateur) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User utilisateur = session.createQuery(
                            "FROM User u WHERE u.nomUtilisateur = :nomUtilisateur", User.class)
                    .setParameter("nomUtilisateur", nomUtilisateur)
                    .uniqueResult();
            return Optional.ofNullable(utilisateur);
        }
    }

    public boolean existeParNomUtilisateur(String nomUtilisateur) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long nombre = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.nomUtilisateur = :nomUtilisateur", Long.class)
                    .setParameter("nomUtilisateur", nomUtilisateur)
                    .uniqueResult();
            return nombre != null && nombre > 0;
        }
    }

    public User sauvegarder(User utilisateur) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(utilisateur);
            transaction.commit();
            return utilisateur;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void mettreAJourStatut(Long idUtilisateur, User.Statut statut) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createMutationQuery(
                            "UPDATE User u SET u.statut = :statut WHERE u.id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", idUtilisateur)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public List<User> trouverTous() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM User u ORDER BY u.nomUtilisateur", User.class).list();
        }
    }

    public List<User> trouverUtilisateursEnLigne() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.statut = :statut ORDER BY u.nomUtilisateur", User.class)
                    .setParameter("statut", User.Statut.EN_LIGNE)
                    .list();
        }
    }
}
