package com.esports.model;

public class CategorieJeu {
    private int id;
    private String nomCategorie;
    private String genre;

    public CategorieJeu() {}

    public CategorieJeu(int id, String nomCategorie, String genre) {
        this.id = id;
        this.nomCategorie = nomCategorie;
        this.genre = genre;
    }

    public CategorieJeu(String nomCategorie, String genre) {
        this.nomCategorie = nomCategorie;
        this.genre = genre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    @Override
    public String toString() {
        return nomCategorie;
    }
}
