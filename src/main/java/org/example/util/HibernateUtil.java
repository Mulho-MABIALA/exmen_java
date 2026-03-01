package org.example.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.example.model.Message;
import org.example.model.User;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration();
            cfg.configure("META-INF/hibernate.cfg.xml");
            cfg.addAnnotatedClass(User.class);
            cfg.addAnnotatedClass(Message.class);
            return cfg.buildSessionFactory();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Hibernate init failed: " + e.getMessage());
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
