package com.esports.service;

import com.esports.interfaces.IUserService;
import com.esports.model.User;
import com.esports.utils.DatabaseConnection;
import com.esports.utils.PasswordUtil;

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
            try { stmt.execute("ALTER TABLE user ADD COLUMN reset_token VARCHAR(10) NULL"); }
            catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user ADD COLUMN reset_token_expiry DATETIME NULL"); }
            catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user ADD COLUMN face_data MEDIUMTEXT NULL"); }
            catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user ADD COLUMN typing_profile TEXT NULL"); }
            catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE user ADD COLUMN typing_biometric_enabled TINYINT(1) DEFAULT 0 NULL"); }
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
            // Hash password if not already hashed
            String pwd = PasswordUtil.isHashed(user.getPassword())
                    ? user.getPassword()
                    : PasswordUtil.hash(user.getPassword());
            stmt.setString(6, pwd); //injection
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
            String pwd = PasswordUtil.isHashed(user.getPassword())
                    ? user.getPassword()
                    : PasswordUtil.hash(user.getPassword());
            stmt.setString(5, pwd);
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
    // PASSWORD RESET — save token
    // ─────────────────────────────
    public boolean saveResetToken(String email, String token, LocalDateTime expiry) {
        String sql = "UPDATE user SET reset_token=?, reset_token_expiry=? WHERE email=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setTimestamp(2, Timestamp.valueOf(expiry));
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] saveResetToken ERROR: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // PASSWORD RESET — find by token (not expired)
    // Expiry is checked in Java to avoid MySQL NOW() timezone issues.
    // ─────────────────────────────
    public Optional<User> findByResetToken(String token) {
        String sql = "SELECT * FROM user WHERE reset_token=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp expiry = null;
                try { expiry = rs.getTimestamp("reset_token_expiry"); } catch (SQLException ignored) {}
                if (expiry == null || expiry.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    System.err.println("[UserService] findByResetToken: token expired or no expiry");
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (Exception e) {
            System.err.println("[UserService] findByResetToken: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ─────────────────────────────
    // PASSWORD RESET — clear token after use
    // ─────────────────────────────
    public boolean clearResetToken(int userId) {
        String sql = "UPDATE user SET reset_token=NULL, reset_token_expiry=NULL WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] clearResetToken: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // PASSWORD RESET — update password only
    // ─────────────────────────────
    public boolean updatePassword(int userId, String hashedPassword) {
        String sql = "UPDATE user SET password=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserService] updatePassword: " + e.getMessage());
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

        String faceData = null;
        try { faceData = rs.getString("face_data"); } catch (SQLException ignored) {}

        User user = new User(
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
        user.setFaceData(faceData);
        return user;
    }
}
