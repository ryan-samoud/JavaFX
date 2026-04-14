package com.esports.model;

public class Jeu {
    private int id;
    private String nom;
    private int trancheAge;
    private String mode;
    private String description;
    private int categorieId;
    private String image;
    private int nbJoueurs;
    private double note;
    private CategorieJeu categorie; // Relation from Symfony

    public Jeu() {}

    public Jeu(int id, String nom, int trancheAge, String mode, String description, int categorieId, String image, int nbJoueurs, double note) {
        this.id = id;
        this.nom = nom;
        this.trancheAge = trancheAge;
        this.mode = mode;
        this.description = description;
        this.categorieId = categorieId;
        this.image = image;
        this.nbJoueurs = nbJoueurs;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getTrancheAge() {
        return trancheAge;
    }

    public void setTrancheAge(int trancheAge) {
        this.trancheAge = trancheAge;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getNbJoueurs() {
        return nbJoueurs;
    }

    public void setNbJoueurs(int nbJoueurs) {
        this.nbJoueurs = nbJoueurs;
    }

    public double getNote() {
        return note;
    }

    public void setNote(double note) {
        this.note = note;
    }

    public CategorieJeu getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieJeu categorie) {
        this.categorie = categorie;
    }
}
