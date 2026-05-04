package com.esports.model;

import java.time.LocalDateTime;

public class Match {

    private int id;
    private int tournoiId;
    private LocalDateTime dateMatch;
    private String round;
    private String statut; // PLANIFIE, EN_COURS, TERMINE
    private String nomJoueur1;
    private String nomJoueur2;
    private int scoreJoueur1;
    private int scoreJoueur2;

    public Match() {
    }

    public Match(int id, int tournoiId, LocalDateTime dateMatch, String round, String statut, 
                 String nomJoueur1, String nomJoueur2, int scoreJoueur1, int scoreJoueur2) {
        this.id = id;
        this.tournoiId = tournoiId;
        this.dateMatch = dateMatch;
        this.round = round;
        this.statut = statut;
        this.nomJoueur1 = nomJoueur1;
        this.nomJoueur2 = nomJoueur2;
        this.scoreJoueur1 = scoreJoueur1;
        this.scoreJoueur2 = scoreJoueur2;
    }

    public Match(int tournoiId, LocalDateTime dateMatch, String round, String statut, 
                 String nomJoueur1, String nomJoueur2, int scoreJoueur1, int scoreJoueur2) {
        this.tournoiId = tournoiId;
        this.dateMatch = dateMatch;
        this.round = round;
        this.statut = statut;
        this.nomJoueur1 = nomJoueur1;
        this.nomJoueur2 = nomJoueur2;
        this.scoreJoueur1 = scoreJoueur1;
        this.scoreJoueur2 = scoreJoueur2;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTournoiId() { return tournoiId; }
    public void setTournoiId(int tournoiId) { this.tournoiId = tournoiId; }
    public LocalDateTime getDateMatch() { return dateMatch; }
    public void setDateMatch(LocalDateTime dateMatch) { this.dateMatch = dateMatch; }
    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getNomJoueur1() { return nomJoueur1; }
    public void setNomJoueur1(String nomJoueur1) { this.nomJoueur1 = nomJoueur1; }
    public String getNomJoueur2() { return nomJoueur2; }
    public void setNomJoueur2(String nomJoueur2) { this.nomJoueur2 = nomJoueur2; }
    public int getScoreJoueur1() { return scoreJoueur1; }
    public void setScoreJoueur1(int scoreJoueur1) { this.scoreJoueur1 = scoreJoueur1; }
    public int getScoreJoueur2() { return scoreJoueur2; }
    public void setScoreJoueur2(int scoreJoueur2) { this.scoreJoueur2 = scoreJoueur2; }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", tournoiId=" + tournoiId +
                ", round='" + round + '\'' +
                ", statut='" + statut + '\'' +
                ", scoreJoueur1=" + scoreJoueur1 +
                ", scoreJoueur2=" + scoreJoueur2 +
                '}';
    }
}
