package com.esports.model;

import java.time.LocalDateTime;

/**
 * MODÈLE — Tournament.java
 * Aligné avec la table 'tournoi' de pi_webjava.
 */
public class Tournament {

    private int id;
    private String nom;
    private String jeu;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String statut; // DRAFT, OUVERT, EN_COURS, TERMINE
    private int nbMatchs;
    private double prize;
    private int nbParticipantsMax;
    private Integer nbParticipantsActuels;
    private String image;
    private String location;

    public Tournament() {}

    public Tournament(int id, String nom, String jeu, LocalDateTime dateDebut, LocalDateTime dateFin,
                      String statut, int nbMatchs, double prize, int nbParticipantsMax,
                      Integer nbParticipantsActuels, String image, String location) {
        this.id = id; this.nom = nom; this.jeu = jeu;
        this.dateDebut = dateDebut; this.dateFin = dateFin;
        this.statut = statut; this.nbMatchs = nbMatchs; this.prize = prize;
        this.nbParticipantsMax = nbParticipantsMax; this.nbParticipantsActuels = nbParticipantsActuels;
        this.image = image; this.location = location;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getJeu() { return jeu; }
    public void setJeu(String jeu) { this.jeu = jeu; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getNbMatchs() { return nbMatchs; }
    public void setNbMatchs(int nbMatchs) { this.nbMatchs = nbMatchs; }
    public double getPrize() { return prize; }
    public void setPrize(double prize) { this.prize = prize; }
    public int getNbParticipantsMax() { return nbParticipantsMax; }
    public void setNbParticipantsMax(int nbParticipantsMax) { this.nbParticipantsMax = nbParticipantsMax; }
    public Integer getNbParticipantsActuels() { return nbParticipantsActuels; }
    public void setNbParticipantsActuels(Integer nbParticipantsActuels) { this.nbParticipantsActuels = nbParticipantsActuels; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // Kept for backward compat (old model used title/prizePool)
    public String getTitle() { return nom; }
    public double getPrizePool() { return prize; }
    public int getMaxTeams() { return nbParticipantsMax; }

    @Override
    public String toString() {
        return "Tournament{id=" + id + ", nom='" + nom + "', jeu='" + jeu + "', statut='" + statut + "', location='" + location + "'}";
    }
}
