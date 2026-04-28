package com.esports.service;

import com.esports.interfaces.IUserService;
import com.esports.model.User;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SERVICE — UserService.java
 * Couche Modèle (MVC) : logique métier + accès aux données pour les utilisateurs.
 */
public class UserService implements IUserService {

    // ─────────────────────────────
    // FIND BY EMAIL
    // ─────────────────────────────
    public Optional<User> findByEmail(String email) {

        String sql = "SELECT * FROM user WHERE email = ? AND is_active = 1";

        try {
             Connection conn = DatabaseConnection.getInstance();
             try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                 stmt.setString(1, email);
                 try (ResultSet rs = stmt.executeQuery()) {
                     if (rs.next()) {
                         return Optional.of(map(rs));
                     }
                 }
             }
         } catch (Exception e) {
             System.err.println("[UserService] findByEmail: " + e.getMessage());
         }

        return Optional.empty();
    }

    // ─────────────────────────────
    // FIND ALL USERS
    // ─────────────────────────────
    public List<User> findAll() {

        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE is_active = 1";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            System.err.println("[UserService] findAll: " + e.getMessage());
        }

        return list;
    }

    // ─────────────────────────────
    // FIND RECENT USERS
    // ─────────────────────────────
    public List<User> findRecent(int limit) {

        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE is_active = 1 ORDER BY date_creation DESC LIMIT ?";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[UserService] findRecent: " + e.getMessage());
        }

        return list;
    }

    // ─────────────────────────────
    // COUNT ACTIVE USERS
    // ─────────────────────────────
    public int countActive() {

        String sql = "SELECT COUNT(*) FROM user WHERE is_active = 1";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[UserService] countActive: " + e.getMessage());
        }

        return 0;
    }

    // ─────────────────────────────
    // DEACTIVATE USER (soft delete)
    // ─────────────────────────────
    public boolean deactivate(int id) {

        String sql = "UPDATE user SET is_active = 0 WHERE id = ?";

        try {
            Connection conn = DatabaseConnection.getInstance();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("[UserService] deactivate: " + e.getMessage());
        }

        return false;
    }

    // ─────────────────────────────
    // MAPPING DB → JAVA
    // ─────────────────────────────
    private User map(ResultSet rs) throws SQLException {

        Timestamp ts = rs.getTimestamp("date_creation");

        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getInt("age"),
                rs.getString("role"),
                rs.getString("password"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getBoolean("is_active")
        );
    }
}
