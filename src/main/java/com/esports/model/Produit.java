package com.esports.model;

/**
 * MODÈLE — Produit.java
 * Représente un produit gaming de la boutique NexUS
 */
public class Produit {

    private int id;
    private String nom;
    private String description;
    private String image;
    private double prix;
    private String stock; // "dispo" ou "non_dispo"
    private String model3d;
    private int idCategoriesProduitId; // ← Nom corrigé selon votre BDD

    // Objet relationnel (pour affichage)
    private CategorieProduit categorie;

    // ─────────────────────────────
    // CONSTRUCTEURS
    // ─────────────────────────────

    /**
     * Constructeur complet (avec ID) - utilisé pour les données depuis la BDD
     */
    public Produit(int id, String nom, String description, String image,
                   double prix, String stock, String model3d,
                   int idCategoriesProduitId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.image = image;
        this.prix = prix;
        this.stock = stock;
        this.model3d = model3d;
        this.idCategoriesProduitId = idCategoriesProduitId;
    }

    /**
     * Constructeur sans ID - utilisé pour la création
     */
    public Produit(String nom, String description, String image,
                   double prix, String stock, String model3d,
                   int idCategoriesProduitId) {
        this.nom = nom;
        this.description = description;
        this.image = image;
        this.prix = prix;
        this.stock = stock;
        this.model3d = model3d;
        this.idCategoriesProduitId = idCategoriesProduitId;
    }

    // ─────────────────────────────
    // GETTERS ET SETTERS
    // ─────────────────────────────

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getModel3d() {
        return model3d;
    }

    public void setModel3d(String model3d) {
        this.model3d = model3d;
    }

    public int getIdCategoriesProduitId() {
        return idCategoriesProduitId;
    }

    public void setIdCategoriesProduitId(int idCategoriesProduitId) {
        this.idCategoriesProduitId = idCategoriesProduitId;
    }

    public CategorieProduit getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieProduit categorie) {
        this.categorie = categorie;
    }

    // ─────────────────────────────
    // MÉTHODES UTILITAIRES
    // ─────────────────────────────

    /**
     * Vérifie si le produit est disponible en stock
     */
    public boolean isDisponible() {
        return "dispo".equalsIgnoreCase(stock);
    }

    /**
     * Retourne le nom de la catégorie (si chargée)
     */
    public String getNomCategorie() {
        return categorie != null ? categorie.getNom() : "N/A";
    }

    /**
     * Retourne le prix formaté
     */
    public String getPrixFormate() {
        return String.format("%.2f DT", prix);
    }

    /**
     * Retourne le statut du stock en français
     */
    public String getStockLabel() {
        return isDisponible() ? "Disponible" : "En rupture";
    }

    @Override
    public String toString() {
        return nom + " - " + getPrixFormate();
    }
}