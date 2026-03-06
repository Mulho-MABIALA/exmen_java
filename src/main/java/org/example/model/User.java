//package org.example.model;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "users")
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true, length = 50)
//    private String username;
//
//    @Column(nullable = false)
//    private String password;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Status status = Status.OFFLINE;
//
//    @Column(name = "date_creation", nullable = false)
//    private LocalDateTime dateCreation = LocalDateTime.now();
//
//    public enum Status { ONLINE, OFFLINE }
//
//    public User() {}
//
//    public User(String username, String password) {
//        this.username = username;
//        this.password = password;
//        this.status = Status.OFFLINE;
//        this.dateCreation = LocalDateTime.now();
//    }
//
//    public Long getId() { return id; }
//    public String getUsername() { return username; }
//    public void setUsername(String username) { this.username = username; }
//    public String getPassword() { return password; }
//    public void setPassword(String password) { this.password = password; }
//    public Status getStatus() { return status; }
//    public void setStatus(Status status) { this.status = status; }
//    public LocalDateTime getDateCreation() { return dateCreation; }
//    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
//}

package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String nomUtilisateur;

    @Column(name = "password", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Statut statut = Statut.HORS_LIGNE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum Statut { EN_LIGNE, HORS_LIGNE }

    public User() {}

    public User(String nomUtilisateur, String motDePasse) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse     = motDePasse;
        this.statut         = Statut.HORS_LIGNE;
        this.dateCreation   = LocalDateTime.now();
    }

    public Long getId()                                    { return id; }
    public String getNomUtilisateur()                      { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur)   { this.nomUtilisateur = nomUtilisateur; }
    public String getMotDePasse()                          { return motDePasse; }
    public void setMotDePasse(String motDePasse)           { this.motDePasse = motDePasse; }
    public Statut getStatut()                              { return statut; }
    public void setStatut(Statut statut)                   { this.statut = statut; }
    public LocalDateTime getDateCreation()                 { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation){ this.dateCreation = dateCreation; }
}