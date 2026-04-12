package com.esports.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MODÈLE — Tournament.java
 */
public class Tournament {

    private int           id;
    private String        title;
    private String        game;
    private double        prizePool;
    private int           maxTeams;
    private String        status;   // UPCOMING, OPEN, IN_PROGRESS, FINISHED
    private LocalDate     startDate;
    private LocalDateTime createdAt;

    public Tournament(int id, String title, String game, double prizePool,
                      int maxTeams, String status, LocalDate startDate, LocalDateTime createdAt) {
        this.id = id; this.title = title; this.game = game;
        this.prizePool = prizePool; this.maxTeams = maxTeams;
        this.status = status; this.startDate = startDate; this.createdAt = createdAt;
    }

    public Tournament(String title, String game, double prizePool, int maxTeams, String status, LocalDate startDate) {
        this.title = title; this.game = game; this.prizePool = prizePool;
        this.maxTeams = maxTeams; this.status = status; this.startDate = startDate;
        this.createdAt = LocalDateTime.now();
    }

    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }
    public String        getTitle()       { return title; }
    public void          setTitle(String t){ this.title = t; }
    public String        getGame()        { return game; }
    public void          setGame(String g){ this.game = g; }
    public double        getPrizePool()   { return prizePool; }
    public void          setPrizePool(double p){ this.prizePool = p; }
    public int           getMaxTeams()    { return maxTeams; }
    public void          setMaxTeams(int m){ this.maxTeams = m; }
    public String        getStatus()      { return status; }
    public void          setStatus(String s){ this.status = s; }
    public LocalDate     getStartDate()   { return startDate; }
    public void          setStartDate(LocalDate d){ this.startDate = d; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void          setCreatedAt(LocalDateTime d){ this.createdAt = d; }

    @Override
    public String toString() {
        return "Tournament{id=" + id + ", title='" + title + "', status='" + status + "'}";
    }
}
