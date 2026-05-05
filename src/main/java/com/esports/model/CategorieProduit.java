package com.esports.model;

import java.time.LocalDateTime;

/**
 * MODÈLE — CategorieProduit.java
 * Représente une catégorie de produits gaming (périphériques, vêtements, accessoires, etc.)
 */
public class CategorieProduit {

    private int id;
    private String nom;
    private String description;
    private LocalDateTime dateCreation;
    private int statut; // 0 = inactif, 1 = actif

    // ─────────────────────────────
    // CONSTRUCTEURS
    // ─────────────────────────────

    /**
     * Constructeur complet (avec ID) - utilisé pour les données depuis la BDD
     */
    public CategorieProduit(int id, String nom, String description,
                            LocalDateTime dateCreation, int statut) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.dateCreation = dateCreation;
        this.statut = statut;
    }

    /**
     * Constructeur sans ID - utilisé pour la création
     */
    public CategorieProduit(String nom, String description) {
        this.nom = nom;
        this.description = description;
        this.statut = 1; // Actif par défaut
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

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getStatut() {
        return statut;
    }

    public void setStatut(int statut) {
        this.statut = statut;
    }

    // ─────────────────────────────
    // MÉTHODES UTILITAIRES
    // ─────────────────────────────

    /**
     * Vérifie si la catégorie est active
     */
    public boolean isActive() {
        return statut == 1;
    }

    @Override
    public String toString() {
        return nom; // Pour affichage dans ComboBox
    }
}