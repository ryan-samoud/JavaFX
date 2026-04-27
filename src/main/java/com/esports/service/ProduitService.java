package com.esports.service;

import com.esports.interfaces.IProduitService;
import com.esports.model.CategorieProduit;
import com.esports.model.Produit;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SERVICE — ProduitService.java
 * Couche Modèle (MVC) : logique métier + accès aux données pour les produits
 */
public class ProduitService implements IProduitService {

    private final CategorieProduitService categorieService = new CategorieProduitService();

    // ─────────────────────────────
    // CREATE - Créer un produit
    // ─────────────────────────────
    @Override
    public boolean create(Produit produit) {
        String sql = "INSERT INTO produit (nom, description, image, prix, stock, model3d, id_categories_produit_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setString(3, produit.getImage());
            stmt.setDouble(4, produit.getPrix());
            stmt.setString(5, produit.getStock());
            stmt.setString(6, produit.getModel3d());
            stmt.setInt(7, produit.getIdCategoriesProduitId());

            int result = stmt.executeUpdate();
            System.out.println("[ProduitService] Produit créé: " + produit.getNom());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("[ProduitService] create: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────
    // READ - Récupérer tous les produits
    // ─────────────────────────────
    @Override
    public List<Produit> findAll() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "ORDER BY p.nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produits.add(mapResultSetWithCategorie(rs));
            }

            System.out.println("[ProduitService] " + produits.size() + " produit(s) trouvé(s)");

        } catch (SQLException e) {
            System.err.println("[ProduitService] findAll: " + e.getMessage());
        }

        return produits;
    }

    // ─────────────────────────────
    // READ - Trouver par ID
    // ─────────────────────────────
    @Override
    public Optional<Produit> findById(int id) {
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "WHERE p.id = ?";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetWithCategorie(rs));
            }

        } catch (SQLException e) {
            System.err.println("[ProduitService] findById: " + e.getMessage());
        }

        return Optional.empty();
    }

    // ─────────────────────────────
    // UPDATE - Mettre à jour un produit
    // ─────────────────────────────
    @Override
    public boolean update(Produit produit) {
        String sql = "UPDATE produit SET nom = ?, description = ?, image = ?, prix = ?, " +
                "stock = ?, model3d = ?, id_categories_produit_id = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setString(3, produit.getImage());
            stmt.setDouble(4, produit.getPrix());
            stmt.setString(5, produit.getStock());
            stmt.setString(6, produit.getModel3d());
            stmt.setInt(7, produit.getIdCategoriesProduitId());
            stmt.setInt(8, produit.getId());

            int result = stmt.executeUpdate();
            System.out.println("[ProduitService] Produit mis à jour: " + produit.getNom());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("[ProduitService] update: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────
    // DELETE - Supprimer un produit
    // ─────────────────────────────
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM produit WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int result = stmt.executeUpdate();

            if (result > 0) {
                System.out.println("[ProduitService] Produit supprimé (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[ProduitService] delete: " + e.getMessage());
        }

        return false;
    }

    // ─────────────────────────────
    // COUNT - Compter les produits
    // ─────────────────────────────
    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM produit";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[ProduitService] count: " + e.getMessage());
        }

        return 0;
    }

    // ─────────────────────────────
    // FIND BY CATEGORIE
    // ─────────────────────────────
    @Override
    public List<Produit> findByCategorie(int categorieId) {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "WHERE p.id_categories_produit_id = ? " +
                "ORDER BY p.nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categorieId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                produits.add(mapResultSetWithCategorie(rs));
            }

            System.out.println("[ProduitService] Catégorie " + categorieId + ": " +
                    produits.size() + " produit(s)");

        } catch (SQLException e) {
            System.err.println("[ProduitService] findByCategorie: " + e.getMessage());
        }

        return produits;
    }

    // ─────────────────────────────
    // SEARCH - Rechercher
    // ─────────────────────────────
    @Override
    public List<Produit> search(String searchTerm) {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "WHERE p.nom LIKE ? OR p.description LIKE ? " +
                "ORDER BY p.nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + searchTerm + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                produits.add(mapResultSetWithCategorie(rs));
            }

            System.out.println("[ProduitService] Recherche '" + searchTerm + "': " +
                    produits.size() + " résultat(s)");

        } catch (SQLException e) {
            System.err.println("[ProduitService] search: " + e.getMessage());
        }

        return produits;
    }

    // ─────────────────────────────
    // FIND AVAILABLE - Produits disponibles
    // ─────────────────────────────
    @Override
    public List<Produit> findAvailable() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "WHERE p.stock = 'dispo' " +
                "ORDER BY p.nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produits.add(mapResultSetWithCategorie(rs));
            }

            System.out.println("[ProduitService] " + produits.size() + " produit(s) disponible(s)");

        } catch (SQLException e) {
            System.err.println("[ProduitService] findAvailable: " + e.getMessage());
        }

        return produits;
    }

    // ─────────────────────────────
    // FIND OUT OF STOCK - Produits en rupture
    // ─────────────────────────────
    @Override
    public List<Produit> findOutOfStock() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, " +
                "       c.nom as categorie_nom, " +
                "       c.description as categorie_desc, " +
                "       c.date_creation as categorie_date, " +
                "       c.statut as categorie_statut " +
                "FROM produit p " +
                "LEFT JOIN categorie_produit c ON p.id_categories_produit_id = c.id " +
                "WHERE p.stock = 'non_dispo' " +
                "ORDER BY p.nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produits.add(mapResultSetWithCategorie(rs));
            }

            System.out.println("[ProduitService] " + produits.size() + " produit(s) en rupture");

        } catch (SQLException e) {
            System.err.println("[ProduitService] findOutOfStock: " + e.getMessage());
        }

        return produits;
    }

    // ─────────────────────────────
    // UNICITÉ — vérifie si un produit identique existe déjà
    // Un produit est considéré comme doublon si même nom (insensible à la casse),
    // même prix et même catégorie. Le paramètre excludeId permet d'exclure
    // le produit en cours de modification.
    // ─────────────────────────────
    public boolean existsByNomPrixCategorie(String nom, double prix, int categorieId, int excludeId) {
        // On compare le nom (insensible à la casse), le prix arrondi à 2 décimales
        // et la catégorie. excludeId = -1 en mode création (ne correspond à aucun id réel).
        String sql = "SELECT COUNT(*) FROM produit " +
                     "WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?)) " +
                     "AND ROUND(prix, 2) = ROUND(?, 2) " +
                     "AND id_categories_produit_id = ? " +
                     "AND id != ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            stmt.setDouble(2, prix);
            stmt.setInt(3, categorieId);
            stmt.setInt(4, excludeId); // -1 en création → ne correspond à aucun id
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[ProduitService] existsByNomPrixCategorie: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // MAPPING - ResultSet → Produit (avec catégorie)
    // ─────────────────────────────
    private Produit mapResultSetWithCategorie(ResultSet rs) throws SQLException {
        Produit produit = new Produit(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getString("image"),
                rs.getDouble("prix"),
                rs.getString("stock"),
                rs.getString("model3d"),
                rs.getInt("id_categories_produit_id")
        );

        // Ajouter l'objet catégorie si disponible
        String categorieNom = rs.getString("categorie_nom");
        if (categorieNom != null) {
            Timestamp ts = rs.getTimestamp("categorie_date");
            CategorieProduit categorie = new CategorieProduit(
                    rs.getInt("id_categories_produit_id"),
                    categorieNom,
                    rs.getString("categorie_desc"),
                    ts != null ? ts.toLocalDateTime() : null,
                    rs.getInt("categorie_statut")
            );
            produit.setCategorie(categorie);
        }

        return produit;
    }
}