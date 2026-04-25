package com.esports.service;

import com.esports.model.Commande;
import com.esports.model.LigneCommande;
import com.esports.model.PanierItem;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CommandeService {

    public CommandeService() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        String sqlCommande = """
            CREATE TABLE IF NOT EXISTS commande (
                id INT AUTO_INCREMENT PRIMARY KEY,
                montant_total DOUBLE NOT NULL,
                statut VARCHAR(30) DEFAULT 'en_attente',
                adresse_livraison VARCHAR(255),
                telephone VARCHAR(20),
                notes TEXT,
                mode_paiement VARCHAR(50),
                date_commande DATETIME DEFAULT CURRENT_TIMESTAMP,
                reference VARCHAR(50) UNIQUE,
                user_id INT DEFAULT 0
            )
        """;
        String sqlLigne = """
            CREATE TABLE IF NOT EXISTS ligne_commande (
                id INT AUTO_INCREMENT PRIMARY KEY,
                quantite INT NOT NULL,
                prix_unitaire DOUBLE NOT NULL,
                commande_id INT NOT NULL,
                produit_id INT NOT NULL,
                FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE
            )
        """;
        try (Connection cn = DatabaseConnection.getInstance();
             Statement st = cn.createStatement()) {
            st.execute(sqlCommande);
            st.execute(sqlLigne);
            System.out.println("[COMMANDE] Tables créées/vérifiées.");
        } catch (SQLException e) {
            System.out.println("[COMMANDE] ERREUR createTables: " + e.getMessage());
        }
    }

    // ── Créer une commande complète depuis le panier ──
    public Commande creerCommande(List<PanierItem> items, double total,
                                  String adresse, String telephone,
                                  String notes, String modePaiement, int userId) {
        String reference = genererReference();
        String sql = """
            INSERT INTO commande 
            (montant_total, statut, adresse_livraison, telephone, notes, mode_paiement, date_commande, reference, user_id)
            VALUES (?, 'payee', ?, ?, ?, ?, NOW(), ?, ?)
        """;
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, total);
            ps.setString(2, adresse);
            ps.setString(3, telephone);
            ps.setString(4, notes);
            ps.setString(5, modePaiement);
            ps.setString(6, reference);
            ps.setInt(7, userId);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int commandeId = keys.getInt(1);
                // Créer les lignes
                for (PanierItem item : items) {
                    insererLigne(commandeId, item);
                }
                Commande c = new Commande();
                c.setId(commandeId);
                c.setMontantTotal(total);
                c.setStatut("payee");
                c.setAdresseLivraison(adresse);
                c.setTelephone(telephone);
                c.setNotes(notes);
                c.setModePaiement(modePaiement);
                c.setReference(reference);
                c.setUserId(userId);
                c.setDateCommande(LocalDateTime.now());
                System.out.println("[COMMANDE] Commande créée ref=" + reference);
                return c;
            }
        } catch (SQLException e) {
            System.out.println("[COMMANDE] ERREUR creerCommande: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void insererLigne(int commandeId, PanierItem item) {
        String sql = "INSERT INTO ligne_commande (quantite, prix_unitaire, commande_id, produit_id) VALUES (?,?,?,?)";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, item.getQuantite());
            ps.setDouble(2, item.getPrixUnitaire());
            ps.setInt(3, commandeId);
            ps.setInt(4, item.getProduitId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[COMMANDE] ERREUR insererLigne: " + e.getMessage());
        }
    }

    private String genererReference() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "NXS-" + uuid;
    }
}