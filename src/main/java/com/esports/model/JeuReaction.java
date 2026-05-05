package com.esports.model;

import java.time.LocalDateTime;

public class JeuReaction {
    public static final String TYPE_HEART = "heart";
    public static final String TYPE_LIKE = "like";
    public static final String TYPE_DISLIKE = "dislike";
    public static final String TYPE_FAVORITE = "favorite";

    private int id;
    private int jeuId;
    private int userId;
    private String type;
    private LocalDateTime createdAt;

    public JeuReaction() {}

    public JeuReaction(int id, int jeuId, int userId, String type, LocalDateTime createdAt) {
        this.id = id;
        this.jeuId = jeuId;
        this.userId = userId;
        this.type = type;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getJeuId() { return jeuId; }
    public void setJeuId(int jeuId) { this.jeuId = jeuId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
