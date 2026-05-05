package com.esports.service;

import com.esports.interfaces.ICategorieProduitService;
import com.esports.model.CategorieProduit;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SERVICE — CategorieProduitService.java
 * Couche Modèle (MVC) : logique métier + accès aux données pour les catégories
 */
public class CategorieProduitService implements ICategorieProduitService {

    // ─────────────────────────────
    // CREATE - Créer une catégorie
    // ─────────────────────────────
    @Override
    public boolean create(CategorieProduit categorie) {
        String sql = "INSERT INTO categorie_produit (nom, description, date_creation, statut) " +
                "VALUES (?, ?, NOW(), 1)";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categorie.getNom());
            stmt.setString(2, categorie.getDescription());

            int result = stmt.executeUpdate();
            System.out.println("[CategorieProduitService] Catégorie créée: " + categorie.getNom());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] create: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────
    // READ - Récupérer toutes les catégories
    // ─────────────────────────────
    @Override
    public List<CategorieProduit> findAll() {
        List<CategorieProduit> categories = new ArrayList<>();
        String sql = "SELECT * FROM categorie_produit WHERE statut = 1 ORDER BY nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(mapResultSet(rs));
            }

            System.out.println("[CategorieProduitService] " + categories.size() + " catégorie(s) trouvée(s)");

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] findAll: " + e.getMessage());
        }

        return categories;
    }

    // ─────────────────────────────
    // READ - Trouver par ID
    // ─────────────────────────────
    @Override
    public Optional<CategorieProduit> findById(int id) {
        String sql = "SELECT * FROM categorie_produit WHERE id = ? AND statut = 1";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] findById: " + e.getMessage());
        }

        return Optional.empty();
    }

    // ─────────────────────────────
    // UPDATE - Mettre à jour une catégorie
    // ─────────────────────────────
    @Override
    public boolean update(CategorieProduit categorie) {
        String sql = "UPDATE categorie_produit SET nom = ?, description = ?, statut = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categorie.getNom());
            stmt.setString(2, categorie.getDescription());
            stmt.setInt(3, categorie.getStatut());
            stmt.setInt(4, categorie.getId());

            int result = stmt.executeUpdate();
            System.out.println("[CategorieProduitService] Catégorie mise à jour: " + categorie.getNom());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] update: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────
    // DELETE - Supprimer (soft delete)
    // ─────────────────────────────
    @Override
    public boolean delete(int id) {
        // Soft delete : on met statut = 0 au lieu de supprimer
        String sql = "UPDATE categorie_produit SET statut = 0 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int result = stmt.executeUpdate();

            if (result > 0) {
                System.out.println("[CategorieProduitService] Catégorie désactivée (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] delete: " + e.getMessage());
        }

        return false;
    }

    // ─────────────────────────────
    // COUNT - Compter les catégories actives
    // ─────────────────────────────
    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM categorie_produit WHERE statut = 1";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] count: " + e.getMessage());
        }

        return 0;
    }

    // ─────────────────────────────
    // SEARCH - Rechercher par nom
    // ─────────────────────────────
    @Override
    public List<CategorieProduit> search(String searchTerm) {
        List<CategorieProduit> categories = new ArrayList<>();
        String sql = "SELECT * FROM categorie_produit " +
                "WHERE (nom LIKE ? OR description LIKE ?) AND statut = 1 " +
                "ORDER BY nom";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String search = "%" + searchTerm + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSet(rs));
            }

            System.out.println("[CategorieProduitService] Recherche '" + searchTerm + "': " +
                    categories.size() + " résultat(s)");

        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] search: " + e.getMessage());
        }

        return categories;
    }

    // ─────────────────────────────
    // UNICITÉ — vérifie si une catégorie avec ce nom existe déjà (insensible à la casse)
    // excludeId permet d'exclure la catégorie en cours de modification.
    // ─────────────────────────────
    public boolean existsByNom(String nom, int excludeId) {
        String sql = "SELECT COUNT(*) FROM categorie_produit " +
                     "WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?)) AND id != ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[CategorieProduitService] existsByNom: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // MAPPING - ResultSet → CategorieProduit
    // ─────────────────────────────
    private CategorieProduit mapResultSet(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("date_creation");

        return new CategorieProduit(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getInt("statut")
        );
    }
}