package com.esports.service;

import com.esports.model.Panier;
import com.esports.model.PanierItem;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierService {

    public PanierService() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        String sqlPanier = """
            CREATE TABLE IF NOT EXISTS panier (
                id INT AUTO_INCREMENT PRIMARY KEY,
                date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
                statut VARCHAR(20) DEFAULT 'ouvert',
                session_id VARCHAR(100),
                utilisateur_id INT DEFAULT 0
            )
        """;
        String sqlItem = """
            CREATE TABLE IF NOT EXISTS panier_item (
                id INT AUTO_INCREMENT PRIMARY KEY,
                quantite INT NOT NULL DEFAULT 1,
                prix_unitaire DOUBLE NOT NULL,
                panier_id INT NOT NULL,
                produit_id INT NOT NULL,
                FOREIGN KEY (panier_id) REFERENCES panier(id) ON DELETE CASCADE
            )
        """;
        try (Connection cn = DatabaseConnection.getInstance();
             Statement st = cn.createStatement()) {
            st.execute(sqlPanier);
            st.execute(sqlItem);
            System.out.println("[PANIER] Tables panier et panier_item vérifiées/créées.");
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR createTablesIfNotExist: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Panier getOrCreatePanier(int utilisateurId) {
        String sessionId = PanierSession.getSessionId();
        System.out.println("[PANIER] getOrCreatePanier userId=" + utilisateurId + " sessionId=" + sessionId);

        String sql = "SELECT * FROM panier WHERE session_id=? AND statut='ouvert' LIMIT 1";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Panier p = mapPanier(rs);
                System.out.println("[PANIER] Panier existant trouvé id=" + p.getId());
                return p;
            }
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR getOrCreatePanier: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[PANIER] Aucun panier trouvé, création...");
        return creerPanier(utilisateurId, sessionId);
    }

    private Panier creerPanier(int utilisateurId, String sessionId) {
        String sql = "INSERT INTO panier (statut, session_id, utilisateur_id) VALUES ('ouvert', ?, ?)";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sessionId);
            ps.setInt(2, utilisateurId);
            int rows = ps.executeUpdate();
            System.out.println("[PANIER] INSERT panier rows=" + rows);
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                Panier p = new Panier();
                p.setId(keys.getInt(1));
                p.setStatut("ouvert");
                p.setSessionId(sessionId);
                p.setUtilisateurId(utilisateurId);
                System.out.println("[PANIER] Nouveau panier créé id=" + p.getId());
                return p;
            }
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR creerPanier: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public int forceCreerPanier(int utilisateurId) {
        String sessionId = PanierSession.getSessionId();
        String sql = "INSERT INTO panier (statut, session_id, utilisateur_id) VALUES ('ouvert', ?, ?)";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sessionId);
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                System.out.println("[PANIER] forceCreerPanier id=" + id);
                return id;
            }
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR forceCreerPanier: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public void ajouterItem(int panierId, int produitId, double prix) {
        System.out.println("[PANIER] ajouterItem panierId=" + panierId + " produitId=" + produitId + " prix=" + prix);

        if (panierId <= 0) {
            System.out.println("[PANIER] ERREUR: panierId invalide (" + panierId + "), abandon.");
            return;
        }

        String check = "SELECT id, quantite FROM panier_item WHERE panier_id=? AND produit_id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(check)) {
            ps.setInt(1, panierId);
            ps.setInt(2, produitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int newQty = rs.getInt("quantite") + 1;
                int itemId = rs.getInt("id");
                System.out.println("[PANIER] Produit déjà présent, incrémente -> " + newQty);
                String upd = "UPDATE panier_item SET quantite=? WHERE id=?";
                try (PreparedStatement pu = cn.prepareStatement(upd)) {
                    pu.setInt(1, newQty);
                    pu.setInt(2, itemId);
                    int rows = pu.executeUpdate();
                    System.out.println("[PANIER] UPDATE quantite rows=" + rows);
                }
                return;
            }
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR check item: " + e.getMessage());
            e.printStackTrace();
        }

        String sql = "INSERT INTO panier_item (quantite, prix_unitaire, panier_id, produit_id) VALUES (1,?,?,?)";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDouble(1, prix);
            ps.setInt(2, panierId);
            ps.setInt(3, produitId);
            int rows = ps.executeUpdate();
            System.out.println("[PANIER] INSERT panier_item rows=" + rows);
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR INSERT panier_item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void supprimerItem(int itemId) {
        System.out.println("[PANIER] supprimerItem id=" + itemId);
        String sql = "DELETE FROM panier_item WHERE id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            int rows = ps.executeUpdate();
            System.out.println("[PANIER] DELETE item rows=" + rows);
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR supprimerItem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void modifierQuantite(int itemId, int nouvelleQuantite) {
        System.out.println("[PANIER] modifierQuantite itemId=" + itemId + " qty=" + nouvelleQuantite);
        if (nouvelleQuantite <= 0) {
            supprimerItem(itemId);
            return;
        }
        String sql = "UPDATE panier_item SET quantite=? WHERE id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, nouvelleQuantite);
            ps.setInt(2, itemId);
            int rows = ps.executeUpdate();
            System.out.println("[PANIER] UPDATE quantite rows=" + rows);
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR modifierQuantite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void viderPanier(int panierId) {
        System.out.println("[PANIER] viderPanier id=" + panierId);
        String sql = "DELETE FROM panier_item WHERE panier_id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            int rows = ps.executeUpdate();
            System.out.println("[PANIER] vider rows=" + rows);
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR viderPanier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<PanierItem> getItems(int panierId) {
        System.out.println("[PANIER] getItems panierId=" + panierId);
        List<PanierItem> items = new ArrayList<>();
        String sql = """
            SELECT pi.*, p.nom, p.image
            FROM panier_item pi
            JOIN produit p ON pi.produit_id = p.id
            WHERE pi.panier_id = ?
        """;
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PanierItem item = new PanierItem();
                item.setId(rs.getInt("id"));
                item.setQuantite(rs.getInt("quantite"));
                item.setPrixUnitaire(rs.getDouble("prix_unitaire"));
                item.setPanierId(rs.getInt("panier_id"));
                item.setProduitId(rs.getInt("produit_id"));
                item.setNomProduit(rs.getString("nom"));
                item.setImageProduit(rs.getString("image"));
                items.add(item);
            }
            System.out.println("[PANIER] getItems trouvé " + items.size() + " items");
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR getItems: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public int getNombreArticles(int panierId) {
        String sql = "SELECT COALESCE(SUM(quantite),0) FROM panier_item WHERE panier_id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int nb = rs.getInt(1);
                System.out.println("[PANIER] getNombreArticles=" + nb);
                return nb;
            }
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR getNombreArticles: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public double getTotal(int panierId) {
        String sql = "SELECT COALESCE(SUM(quantite * prix_unitaire),0) FROM panier_item WHERE panier_id=?";
        try (Connection cn = DatabaseConnection.getInstance();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.out.println("[PANIER] ERREUR getTotal: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    private Panier mapPanier(ResultSet rs) throws SQLException {
        Panier p = new Panier();
        p.setId(rs.getInt("id"));
        p.setStatut(rs.getString("statut"));
        p.setSessionId(rs.getString("session_id"));
        p.setUtilisateurId(rs.getInt("utilisateur_id"));
        return p;
    }
}