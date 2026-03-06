//package org.example.model;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "messages")
//public class Message {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "sender_id", nullable = false)
//    private User sender;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "receiver_id", nullable = false)
//    private User receiver;
//
//    @Column(nullable = false, length = 1000)
//    private String contenu;
//
//    @Column(name = "date_envoi", nullable = false)
//    private LocalDateTime dateEnvoi = LocalDateTime.now();
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Statut statut = Statut.ENVOYE;
//
//    public enum Statut { ENVOYE, RECU, LU }
//
//    public Message() {}
//
//    public Message(User sender, User receiver, String contenu) {
//        this.sender = sender;
//        this.receiver = receiver;
//        this.contenu = contenu;
//        this.dateEnvoi = LocalDateTime.now();
//        this.statut = Statut.ENVOYE;
//    }
//
//    public Long getId() { return id; }
//    public User getSender() { return sender; }
//    public void setSender(User sender) { this.sender = sender; }
//    public User getReceiver() { return receiver; }
//    public void setReceiver(User receiver) { this.receiver = receiver; }
//    public String getContenu() { return contenu; }
//    public void setContenu(String contenu) { this.contenu = contenu; }
//    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
//    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
//    public Statut getStatut() { return statut; }
//    public void setStatut(Statut statut) { this.statut = statut; }
//}

package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User destinataire;

    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ENVOYE;

    public enum Statut { ENVOYE, RECU, LU }

    public Message() {}

    public Message(User expediteur, User destinataire, String contenu) {
        this.expediteur   = expediteur;
        this.destinataire = destinataire;
        this.contenu      = contenu;
        this.dateEnvoi    = LocalDateTime.now();
        this.statut       = Statut.ENVOYE;
    }

    public Long getId()                              { return id; }
    public User getExpediteur()                      { return expediteur; }
    public void setExpediteur(User expediteur)       { this.expediteur = expediteur; }
    public User getDestinataire()                    { return destinataire; }
    public void setDestinataire(User destinataire)   { this.destinataire = destinataire; }
    public String getContenu()                       { return contenu; }
    public void setContenu(String contenu)           { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi()              { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi){ this.dateEnvoi = dateEnvoi; }
    public Statut getStatut()                        { return statut; }
    public void setStatut(Statut statut)             { this.statut = statut; }
}
