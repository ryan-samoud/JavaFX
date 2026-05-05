package com.esports.service;

import com.esports.interfaces.ISponsorService;
import com.esports.model.Sponsor;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE — SponsorService.java
 *
 * Table SQL :
 *   CREATE TABLE sponsor (
 *     id           INT AUTO_INCREMENT PRIMARY KEY,
 *     nom          VARCHAR(100) NOT NULL,
 *     type         ENUM('humain','entreprise') NOT NULL DEFAULT 'entreprise',
 *     email        VARCHAR(150),
 *     tel          VARCHAR(20),
 *     prix         DECIMAL(10,2) DEFAULT 0.00,
 *     evenement_id INT NOT NULL,
 *     FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE
 *   );
 */
public class SponsorService implements ISponsorService {

    public List<Sponsor> findAll() {
        List<Sponsor> list = new ArrayList<>();
        String sql = "SELECT * FROM sponsor ORDER BY nom";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[SponsorService] findAll : " + e.getMessage());
        }
        return list;
    }

    public List<Sponsor> findByEvenement(int evenementId) {
        List<Sponsor> list = new ArrayList<>();
        String sql = "SELECT * FROM sponsor WHERE evenement_id = ? ORDER BY nom";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, evenementId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[SponsorService] findByEvenement : " + e.getMessage());
        }
        return list;
    }

    public boolean save(Sponsor s) {
        String sql = "INSERT INTO sponsor (nom, type, email, tel, prix, evenement_id) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, s.getNom());
            stmt.setString(2, s.getType());
            stmt.setString(3, s.getEmail());
            stmt.setString(4, s.getTel());
            stmt.setDouble(5, s.getPrix());
            stmt.setInt(6, s.getEvenementId());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) s.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[SponsorService] save : " + e.getMessage());
        }
        return false;
    }

    public boolean update(Sponsor s) {
        String sql = "UPDATE sponsor SET nom=?, type=?, email=?, tel=?, prix=?, evenement_id=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, s.getNom());
            stmt.setString(2, s.getType());
            stmt.setString(3, s.getEmail());
            stmt.setString(4, s.getTel());
            stmt.setDouble(5, s.getPrix());
            stmt.setInt(6, s.getEvenementId());
            stmt.setInt(7, s.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SponsorService] update : " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM sponsor WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SponsorService] delete : " + e.getMessage());
        }
        return false;
    }

    private Sponsor map(ResultSet rs) throws SQLException {
        return new Sponsor(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("type"),
                rs.getString("email"),
                rs.getString("tel"),
                rs.getDouble("prix"),
                rs.getInt("evenement_id")
        );
    }
}
