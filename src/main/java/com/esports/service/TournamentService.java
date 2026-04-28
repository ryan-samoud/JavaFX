package com.esports.service;

import com.esports.interfaces.ITournamentService;
import com.esports.model.Tournament;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE — TournamentService.java
 * Couche Modèle (MVC) : logique métier + accès aux données pour les tournois.
 *
 * Table SQL :
 *   CREATE TABLE tournaments (
 *     id          INT AUTO_INCREMENT PRIMARY KEY,
 *     title       VARCHAR(100) NOT NULL,
 *     game        VARCHAR(50)  NOT NULL,
 *     prize_pool  DECIMAL(10,2) DEFAULT 0.00,
 *     max_teams   INT NOT NULL DEFAULT 16,
 *     status      ENUM('UPCOMING','OPEN','IN_PROGRESS','FINISHED') DEFAULT 'UPCOMING',
 *     start_date  DATE NULL,
 *     created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
 *   );
 */
public class TournamentService implements ITournamentService {

    public List<Tournament> findAll() {
        List<Tournament> list = new ArrayList<>();
        String sql = "SELECT * FROM tournaments ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) {
            System.err.println("[TournamentService] findAll : " + e.getMessage());
        }
        return list;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM tournaments";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[TournamentService] countAll : " + e.getMessage());
        }
        return 0;
    }

    public boolean save(Tournament t) {
        String sql = "INSERT INTO tournaments (title, game, prize_pool, max_teams, status, start_date) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, t.getTitle());
            stmt.setString(2, t.getGame());
            stmt.setDouble(3, t.getPrizePool());
            stmt.setInt(4, t.getMaxTeams());
            stmt.setString(5, t.getStatus());
            stmt.setDate(6, t.getStartDate() != null ? Date.valueOf(t.getStartDate()) : null);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) t.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[TournamentService] save : " + e.getMessage());
        }
        return false;
    }

    public boolean update(Tournament t) {
        String sql = "UPDATE tournaments SET title=?, game=?, prize_pool=?, max_teams=?, status=?, start_date=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, t.getTitle());
            stmt.setString(2, t.getGame());
            stmt.setDouble(3, t.getPrizePool());
            stmt.setInt(4, t.getMaxTeams());
            stmt.setString(5, t.getStatus());
            stmt.setDate(6, t.getStartDate() != null ? Date.valueOf(t.getStartDate()) : null);
            stmt.setInt(7, t.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TournamentService] update : " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM tournaments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TournamentService] delete : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────
    // MAPPING DB → JAVA
    // ─────────────────────────────
    private Tournament map(ResultSet rs) throws SQLException {
        Date sd = rs.getDate("start_date");
        return new Tournament(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("game"),
                rs.getDouble("prize_pool"),
                rs.getInt("max_teams"),
                rs.getString("status"),
                sd != null ? sd.toLocalDate() : null,
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
