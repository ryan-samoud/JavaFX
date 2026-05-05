package com.esports.model;

import java.time.LocalDateTime;

public class Commande {
    private int id;
    private double montantTotal;
    private String statut;
    private String adresseLivraison;
    private String telephone;
    private String notes;
    private String modePaiement;
    private LocalDateTime dateCommande;
    private String reference;
    private int userId;

    public Commande() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}