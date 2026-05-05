package com.esports.model;

import java.time.LocalDateTime;

public class Panier {
    private int id;
    private LocalDateTime dateCreation;
    private String statut; // "ouvert", "validé", "abandonné"
    private String sessionId;
    private int utilisateurId;

    public Panier() {}

    public Panier(String sessionId, int utilisateurId) {
        this.sessionId = sessionId;
        this.utilisateurId = utilisateurId;
        this.statut = "ouvert";
        this.dateCreation = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
}