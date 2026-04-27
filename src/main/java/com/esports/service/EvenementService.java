package com.esports.service;

import com.esports.interfaces.IEvenementService;
import com.esports.model.Evenement;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE — EvenementService.java
 *
 * Table SQL (à créer via create_tables.sql) :
 *   CREATE TABLE evenement (
 *     id              INT AUTO_INCREMENT PRIMARY KEY,
 *     nom             VARCHAR(100) NOT NULL,
 *     description     TEXT,
 *     date            DATE NOT NULL,
 *     lieu            VARCHAR(200) NOT NULL,
 *     nbr_participant INT DEFAULT 0,
 *     image           VARCHAR(255),
 *     created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
 *   );
 */
public class EvenementService implements IEvenementService {

    public List<Evenement> findAll() {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[EvenementService] findAll ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public Evenement findById(int id) {
        String sql = "SELECT * FROM evenement WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("[EvenementService] findById ERROR : " + e.getMessage());
        }
        return null;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[EvenementService] countAll ERROR : " + e.getMessage());
        }
        return 0;
    }

    public boolean save(Evenement e) {
        String sql = "INSERT INTO evenement (nom, description, date, lieu, nbr_participant, image) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, e.getNom());
            stmt.setString(2, e.getDescription());
            stmt.setDate(3, Date.valueOf(e.getDate()));
            stmt.setString(4, e.getLieu());
            stmt.setInt(5, e.getNbrParticipant());
            stmt.setString(6, e.getImage());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) e.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException ex) {
            System.err.println("[EvenementService] save ERROR : " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    public boolean update(Evenement e) {
        String sql = "UPDATE evenement SET nom=?, description=?, date=?, lieu=?, nbr_participant=?, image=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getNom());
            stmt.setString(2, e.getDescription());
            stmt.setDate(3, Date.valueOf(e.getDate()));
            stmt.setString(4, e.getLieu());
            stmt.setInt(5, e.getNbrParticipant());
            stmt.setString(6, e.getImage());
            stmt.setInt(7, e.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[EvenementService] update ERROR : " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    public boolean delete(int id) {
        String deleteSponsors = "DELETE FROM sponsor WHERE evenement_id = ?";
        String deleteEvent = "DELETE FROM evenement WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance()) {

            PreparedStatement ps1 = conn.prepareStatement(deleteSponsors);
            ps1.setInt(1, id);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement(deleteEvent);
            ps2.setInt(1, id);

            return ps2.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Evenement map(ResultSet rs) throws SQLException {
        Date d       = rs.getDate("date");
        return new Evenement(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                d  != null ? d.toLocalDate()           : null,
                rs.getString("lieu"),
                rs.getInt("nbr_participant"),
                rs.getString("image")
        );
    }
    public void incrementParticipants(int id) {
        String sql = "UPDATE evenement SET nbr_participant = nbr_participant + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void decrementParticipants(int id) {
        String sql = "UPDATE evenement SET nbr_participant = CASE WHEN nbr_participant > 0 THEN nbr_participant - 1 ELSE 0 END WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}