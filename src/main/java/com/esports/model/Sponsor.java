package com.esports.model;

/**
 * MODÈLE — Sponsor.java
 */
public class Sponsor {

    private int    id;
    private String nom;
    private String type;   // "humain" | "entreprise"
    private String email;
    private String tel;
    private double prix;
    private int    evenementId;

    public Sponsor(int id, String nom, String type, String email,
                   String tel, double prix, int evenementId) {
        this.id          = id;
        this.nom         = nom;
        this.type        = type;
        this.email       = email;
        this.tel         = tel;
        this.prix        = prix;
        this.evenementId = evenementId;
    }

    public Sponsor(String nom, String type, String email,
                   String tel, double prix, int evenementId) {
        this.nom         = nom;
        this.type        = type;
        this.email       = email;
        this.tel         = tel;
        this.prix        = prix;
        this.evenementId = evenementId;
    }

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }
    public String getNom()             { return nom; }
    public void   setNom(String n)     { this.nom = n; }
    public String getType()            { return type; }
    public void   setType(String t)    { this.type = t; }
    public String getEmail()           { return email; }
    public void   setEmail(String e)   { this.email = e; }
    public String getTel()             { return tel; }
    public void   setTel(String t)     { this.tel = t; }
    public double getPrix()            { return prix; }
    public void   setPrix(double p)    { this.prix = p; }
    public int    getEvenementId()     { return evenementId; }
    public void   setEvenementId(int e){ this.evenementId = e; }

    public String getTypeLabel() {
        return "humain".equals(type) ? "Particulier" : "Entreprise";
    }

    @Override
    public String toString() {
        return "Sponsor{id=" + id + ", nom='" + nom + "', type='" + type + "'}";
    }
}
