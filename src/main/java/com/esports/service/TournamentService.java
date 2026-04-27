package com.esports.service;

import com.esports.interfaces.ITournamentService;
import com.esports.model.Tournament;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE — TournamentService.java
 * Utilise la table 'tournoi' (schéma pi_webjava).
 */
public class TournamentService implements ITournamentService {

    @Override
    public List<Tournament> findAll() {
        List<Tournament> list = new ArrayList<>();
        String sql = "SELECT * FROM tournoi ORDER BY date_debut DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println("[TournamentService] findAll : " + e.getMessage()); }
        return list;
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM tournoi";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("[TournamentService] countAll : " + e.getMessage()); }
        return 0;
    }

    @Override
    public boolean save(Tournament t) {
        String sql = "INSERT INTO tournoi (nom, jeu, date_debut, date_fin, statut, nb_matchs, prize, nbParticipantsMax, nbParticipantsActuels, image, location) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, t.getNom()); stmt.setString(2, t.getJeu());
            stmt.setTimestamp(3, t.getDateDebut() != null ? Timestamp.valueOf(t.getDateDebut()) : null);
            stmt.setTimestamp(4, t.getDateFin() != null ? Timestamp.valueOf(t.getDateFin()) : null);
            stmt.setString(5, t.getStatut()); stmt.setInt(6, t.getNbMatchs());
            stmt.setDouble(7, t.getPrize()); stmt.setInt(8, t.getNbParticipantsMax());
            if (t.getNbParticipantsActuels() != null) stmt.setInt(9, t.getNbParticipantsActuels());
            else stmt.setNull(9, Types.INTEGER);
            stmt.setString(10, t.getImage() != null ? t.getImage() : "");
            stmt.setString(11, t.getLocation() != null ? t.getLocation() : "");
            if (stmt.executeUpdate() > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) t.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) { System.err.println("[TournamentService] save : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean update(Tournament t) {
        String sql = "UPDATE tournoi SET nom=?, jeu=?, date_debut=?, date_fin=?, statut=?, nb_matchs=?, prize=?, nbParticipantsMax=?, nbParticipantsActuels=?, image=?, location=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, t.getNom()); stmt.setString(2, t.getJeu());
            stmt.setTimestamp(3, t.getDateDebut() != null ? Timestamp.valueOf(t.getDateDebut()) : null);
            stmt.setTimestamp(4, t.getDateFin() != null ? Timestamp.valueOf(t.getDateFin()) : null);
            stmt.setString(5, t.getStatut()); stmt.setInt(6, t.getNbMatchs());
            stmt.setDouble(7, t.getPrize()); stmt.setInt(8, t.getNbParticipantsMax());
            if (t.getNbParticipantsActuels() != null) stmt.setInt(9, t.getNbParticipantsActuels());
            else stmt.setNull(9, Types.INTEGER);
            stmt.setString(10, t.getImage() != null ? t.getImage() : "");
            stmt.setString(11, t.getLocation() != null ? t.getLocation() : "");
            stmt.setInt(12, t.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[TournamentService] update : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM tournoi WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[TournamentService] delete : " + e.getMessage()); }
        return false;
    }

    @Override
    public Tournament findById(int id) {
        String sql = "SELECT * FROM tournoi WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) { System.err.println("[TournamentService] findById : " + e.getMessage()); }
        return null;
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM tournoi WHERE nom = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { System.err.println("[TournamentService] existsByName : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean existsByNameExcludeId(String name, int id) {
        String sql = "SELECT COUNT(*) FROM tournoi WHERE nom = ? AND id != ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { System.err.println("[TournamentService] existsByNameExcludeId : " + e.getMessage()); }
        return false;
    }

    private Tournament map(ResultSet rs) throws SQLException {
        Timestamp dd = rs.getTimestamp("date_debut");
        Timestamp df = rs.getTimestamp("date_fin");
        String loc = "";
        try { loc = rs.getString("location"); } catch (SQLException ignored) {}
        return new Tournament(
                rs.getInt("id"), rs.getString("nom"), rs.getString("jeu"),
                dd != null ? dd.toLocalDateTime() : null,
                df != null ? df.toLocalDateTime() : null,
                rs.getString("statut"), rs.getInt("nb_matchs"), rs.getDouble("prize"),
                rs.getInt("nbParticipantsMax"), (Integer) rs.getObject("nbParticipantsActuels"),
                rs.getString("image"), loc
        );
    }
}
