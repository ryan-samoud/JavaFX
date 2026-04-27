package com.esports.model;

import java.time.LocalDateTime;

public class TournamentInscription {
    private int id;
    private int tournoiId;
    private int playerId;
    private int rating;
    private LocalDateTime createdAt;
    private int points;

    public TournamentInscription() {}

    public TournamentInscription(int tournoiId, int playerId) {
        this.tournoiId = tournoiId;
        this.playerId = playerId;
        this.rating = 1000;
        this.createdAt = LocalDateTime.now();
        this.points = 0;
    }

    public TournamentInscription(int id, int tournoiId, int playerId, int rating, LocalDateTime createdAt, int points) {
        this.id = id; this.tournoiId = tournoiId; this.playerId = playerId;
        this.rating = rating; this.createdAt = createdAt; this.points = points;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTournoiId() { return tournoiId; }
    public void setTournoiId(int tournoiId) { this.tournoiId = tournoiId; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
