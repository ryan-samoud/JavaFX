package com.esports.model;

import javafx.beans.property.*;

/**
 * MODEL — Entité Joueur.
 * Utilise les JavaFX Properties pour le binding automatique avec la View.
 */
public class Player {

    private final IntegerProperty id;
    private final StringProperty  pseudo;
    private final StringProperty  role;
    private final StringProperty  team;
    private final IntegerProperty rank;
    private final DoubleProperty  winRate;
    private final StringProperty  status; // "ONLINE" | "IN_GAME" | "OFFLINE"

    public Player(int id, String pseudo, String role, String team, int rank, double winRate, String status) {
        this.id      = new SimpleIntegerProperty(id);
        this.pseudo  = new SimpleStringProperty(pseudo);
        this.role    = new SimpleStringProperty(role);
        this.team    = new SimpleStringProperty(team);
        this.rank    = new SimpleIntegerProperty(rank);
        this.winRate = new SimpleDoubleProperty(winRate);
        this.status  = new SimpleStringProperty(status);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()               { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getPseudo()             { return pseudo.get(); }
    public void   setPseudo(String v)     { pseudo.set(v); }
    public StringProperty pseudoProperty() { return pseudo; }

    public String getRole()               { return role.get(); }
    public void   setRole(String v)       { role.set(v); }
    public StringProperty roleProperty()  { return role; }

    public String getTeam()               { return team.get(); }
    public void   setTeam(String v)       { team.set(v); }
    public StringProperty teamProperty()  { return team; }

    public int    getRank()               { return rank.get(); }
    public void   setRank(int v)          { rank.set(v); }
    public IntegerProperty rankProperty() { return rank; }

    public double getWinRate()            { return winRate.get(); }
    public void   setWinRate(double v)    { winRate.set(v); }
    public DoubleProperty winRateProperty() { return winRate; }

    public String getStatus()             { return status.get(); }
    public void   setStatus(String v)     { status.set(v); }
    public StringProperty statusProperty() { return status; }

    @Override
    public String toString() {
        return pseudo.get() + " [" + team.get() + "] - " + role.get();
    }
}
