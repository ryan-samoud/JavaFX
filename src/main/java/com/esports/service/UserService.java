package com.esports.service;

import com.esports.interfaces.IUserService;
import com.esports.model.User;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SERVICE — UserService.java
 * Couche Modèle (MVC) : logique métier + accès aux données pour les utilisateurs.
 */
public class UserService implements IUserService {

    public UserService() {
        ensureColumns();
    }

    // ─────────────────────────────
    // SCHEMA MIGRATION
    // ─────────────────────────────
    private void ensureColumns() {
        try (Connection conn = DatabaseConnection.getInstance();
             Statement stmt = conn.createStatement()) {
            try { stmt.execute("ALTER TABLE user ADD COLUMN ban_reason VARCHAR(500) NULL"); }
            catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user ADD COLUMN suspended_until DATETIME NULL"); }
            catch (SQLException ignored) {}
        } catch (Exception e) {
            System.err.println("[UserService] ensureColumns: " + e.getMessage());
        }
    }

    // ─────────────────────────────
    // FIND BY EMAIL (active only)
    // ─────────────────────────────
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ? AND is_active = 1";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (Exception e) {
            System.err.println("[UserService] findByEmail: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ─────────────────────────────
    // FIND BY EMAIL (any status)
    // ─────────────────────────────
    public Optional<User> findByEmailAny(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (Exception e) {
            System.err.println("[UserService] findByEmailAny: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ─────────────────────────────
    // FIND ALL ACTIVE USERS
    // ─────────────────────────────
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE is_active = 1";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("[UserService] findAll: " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────
    // FIND ALL USERS (incl. banned/suspended)
    // ─────────────────────────────
    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY date_creation DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("[UserService] findAllUsers: " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────
    // FIND RECENT USERS
    // ─────────────────────────────
    public List<User> findRecent(int limit) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE is_active = 1 ORDER BY date_creation DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(map(rs));
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
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[UserService] countActive: " + e.getMessage());
        }
        return 0;
    }

    // ─────────────────────────────
    // SAVE NEW USER
    // ─────────────────────────────
    public boolean save(User user) {
        String sql = "INSERT INTO user (nom, prenom, email, age, role, password, is_active, date_creation) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setInt(4,    user.getAge());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getPassword());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] save: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // UPDATE USER
    // ─────────────────────────────
    public boolean update(User user) {
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, age=?, password=?, photo=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setInt(4,    user.getAge());
            stmt.setString(5, user.getPassword());
            stmt.setString(6, user.getPhoto());
            stmt.setInt(7,    user.getId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] update: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // DEACTIVATE USER (soft delete)
    // ─────────────────────────────
    public boolean deactivate(int id) {
        String sql = "UPDATE user SET is_active = 0 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] deactivate: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // BAN USER (permanent)
    // ─────────────────────────────
    public boolean ban(int id, String reason) {
        String sql = "UPDATE user SET is_active=0, ban_reason=?, suspended_until=NULL WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] ban: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // UNBAN USER
    // ─────────────────────────────
    public boolean unban(int id) {
        String sql = "UPDATE user SET is_active=1, ban_reason=NULL, suspended_until=NULL WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] unban: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // SUSPEND USER (temporary)
    // ─────────────────────────────
    public boolean suspend(int id, LocalDateTime until, String reason) {
        String sql = "UPDATE user SET is_active=0, suspended_until=?, ban_reason=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(until));
            stmt.setString(2, reason);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] suspend: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // MAPPING DB → JAVA
    // ─────────────────────────────
    private User map(ResultSet rs) throws SQLException {
        Timestamp ts  = rs.getTimestamp("date_creation");
        Timestamp sus = null;
        String banReason = null;
        try { sus = rs.getTimestamp("suspended_until"); } catch (SQLException ignored) {}
        try { banReason = rs.getString("ban_reason"); }   catch (SQLException ignored) {}

        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getInt("age"),
                rs.getString("role"),
                rs.getString("password"),
                ts  != null ? ts.toLocalDateTime()  : null,
                rs.getBoolean("is_active"),
                rs.getString("photo"),
                banReason,
                sus != null ? sus.toLocalDateTime() : null
        );
    }
}
