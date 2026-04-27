package com.esports.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MODÈLE — Evenement.java
 */
public class Evenement {

    private int           id;
    private String        nom;
    private String        description;
    private LocalDate     date;
    private String        lieu;
    private int           nbrParticipant;
    private String        image;

    public Evenement(int id, String nom, String description, LocalDate date,
                     String lieu, int nbrParticipant, String image) {
        this.id            = id;
        this.nom           = nom;
        this.description   = description;
        this.date          = date;
        this.lieu          = lieu;
        this.nbrParticipant = nbrParticipant;
        this.image         = image;
    }

    public Evenement(String nom, String description, LocalDate date,
                     String lieu, int nbrParticipant, String image) {
        this.nom           = nom;
        this.description   = description;
        this.date          = date;
        this.lieu          = lieu;
        this.nbrParticipant = nbrParticipant;
        this.image         = image;
    }

    public int           getId()              { return id; }
    public void          setId(int id)        { this.id = id; }
    public String        getNom()             { return nom; }
    public void          setNom(String n)     { this.nom = n; }
    public String        getDescription()     { return description; }
    public void          setDescription(String d) { this.description = d; }
    public LocalDate     getDate()            { return date; }
    public void          setDate(LocalDate d) { this.date = d; }
    public String        getLieu()            { return lieu; }
    public void          setLieu(String l)    { this.lieu = l; }
    public int           getNbrParticipant()  { return nbrParticipant; }
    public void          setNbrParticipant(int n) { this.nbrParticipant = n; }
    public String        getImage()           { return image; }
    public void          setImage(String i)   { this.image = i; }

    public boolean isPast() {
        return date != null && date.isBefore(LocalDate.now());
    }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", nom='" + nom + "', date=" + date + "}";
    }
}
