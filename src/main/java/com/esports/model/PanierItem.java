package com.esports.model;

public class PanierItem {
    private int id;
    private int quantite;
    private double prixUnitaire;
    private int panierId;
    private int produitId;
    private String nomProduit;
    private String imageProduit;

    public PanierItem() {}

    public PanierItem(int quantite, double prixUnitaire, int panierId, int produitId) {
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.panierId = panierId;
        this.produitId = produitId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public int getPanierId() { return panierId; }
    public void setPanierId(int panierId) { this.panierId = panierId; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public String getImageProduit() { return imageProduit; }
    public void setImageProduit(String imageProduit) { this.imageProduit = imageProduit; }

    public double getTotalLigne() { return quantite * prixUnitaire; }
}