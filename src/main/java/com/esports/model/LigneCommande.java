package com.esports.model;

public class LigneCommande {
    private int id;
    private int quantite;
    private double prixUnitaire;
    private int commandeId;
    private int produitId;
    private String nomProduit;

    public LigneCommande() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public double getTotalLigne() { return quantite * prixUnitaire; }
}
