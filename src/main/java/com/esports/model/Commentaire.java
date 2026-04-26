package com.esports.model;

import java.time.LocalDateTime;

public class Commentaire {

    private int           id;
    private String        contenu;
    private LocalDateTime createdAt;
    private int           evenementId;
    private int           userId;
    private String        auteurNom;
    private boolean       flagged;

    public Commentaire(int id, String contenu, LocalDateTime createdAt,
                       int evenementId, int userId, String auteurNom, boolean flagged) {
        this.id          = id;
        this.contenu     = contenu;
        this.createdAt   = createdAt;
        this.evenementId = evenementId;
        this.userId      = userId;
        this.auteurNom   = auteurNom;
        this.flagged     = flagged;
    }

    public Commentaire(String contenu, int evenementId, int userId) {
        this.contenu     = contenu;
        this.evenementId = evenementId;
        this.userId      = userId;
        this.createdAt   = LocalDateTime.now();
        this.flagged     = false;
    }

    public int           getId()              { return id; }
    public void          setId(int id)        { this.id = id; }
    public String        getContenu()         { return contenu; }
    public void          setContenu(String c) { this.contenu = c; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void          setCreatedAt(LocalDateTime d) { this.createdAt = d; }
    public int           getEvenementId()     { return evenementId; }
    public void          setEvenementId(int e){ this.evenementId = e; }
    public int           getUserId()          { return userId; }
    public void          setUserId(int u)     { this.userId = u; }
    public String        getAuteurNom()       { return auteurNom; }
    public void          setAuteurNom(String a){ this.auteurNom = a; }
    public boolean       isFlagged()          { return flagged; }
    public void          setFlagged(boolean f){ this.flagged = f; }
}
