package com.esports.service;

import com.esports.interfaces.ICategorieJeuService;
import com.esports.model.CategorieJeu;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieJeuService implements ICategorieJeuService {

    @Override
    public boolean add(CategorieJeu c) {
        String sql = "INSERT INTO categorie_jeu (nom_categorie, genre) VALUES (?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, c.getNomCategorie());
                stmt.setString(2, c.getGenre());
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] add: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(CategorieJeu c) {
        String sql = "UPDATE categorie_jeu SET nom_categorie = ?, genre = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, c.getNomCategorie());
                stmt.setString(2, c.getGenre());
                stmt.setInt(3, c.getId());
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] update: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM categorie_jeu WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] delete: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<CategorieJeu> findAll() {
        List<CategorieJeu> list = new ArrayList<>();
        String sql = "SELECT * FROM categorie_jeu";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new CategorieJeu(
                            rs.getInt("id"),
                            rs.getString("nom_categorie"),
                            rs.getString("genre")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] findAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public CategorieJeu findById(int id) {
        String sql = "SELECT * FROM categorie_jeu WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new CategorieJeu(
                                rs.getInt("id"),
                                rs.getString("nom_categorie"),
                                rs.getString("genre")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean existsByName(String nom, int excludeId) {
        String sql = "SELECT COUNT(*) FROM categorie_jeu WHERE LOWER(nom_categorie) = LOWER(?) AND id != ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nom);
                stmt.setInt(2, excludeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategorieJeuService] existsByName: " + e.getMessage());
        }
        return false;
    }
}
