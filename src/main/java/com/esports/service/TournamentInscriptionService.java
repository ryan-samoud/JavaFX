package com.esports.service;

import com.esports.interfaces.ITournamentInscriptionService;
import com.esports.model.TournamentInscription;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournamentInscriptionService implements ITournamentInscriptionService {

    @Override
    public boolean register(TournamentInscription ti) {
        String sql = "INSERT INTO tournoi_inscription (tournoi_id, player_id, rating, created_at, points) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ti.getTournoiId()); stmt.setInt(2, ti.getPlayerId());
            stmt.setInt(3, ti.getRating());
            stmt.setTimestamp(4, Timestamp.valueOf(ti.getCreatedAt()));
            stmt.setInt(5, ti.getPoints());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] register : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean isRegistered(int tournoiId, int playerId) {
        String sql = "SELECT * FROM tournoi_inscription WHERE tournoi_id = ? AND player_id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tournoiId); stmt.setInt(2, playerId);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] isRegistered : " + e.getMessage()); }
        return false;
    }

    @Override
    public List<TournamentInscription> findByTournament(int tid) {
        List<TournamentInscription> list = new ArrayList<>();
        String sql = "SELECT * FROM tournoi_inscription WHERE tournoi_id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] findByTournament : " + e.getMessage()); }
        return list;
    }

    @Override
    public List<TournamentInscription> findByPlayer(int pid) {
        List<TournamentInscription> list = new ArrayList<>();
        String sql = "SELECT * FROM tournoi_inscription WHERE player_id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] findByPlayer : " + e.getMessage()); }
        return list;
    }

    @Override
    public boolean unregister(int id) {
        String sql = "DELETE FROM tournoi_inscription WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] unregister : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean update(TournamentInscription ti) {
        String sql = "UPDATE tournoi_inscription SET tournoi_id=?, player_id=?, rating=?, points=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ti.getTournoiId()); stmt.setInt(2, ti.getPlayerId());
            stmt.setInt(3, ti.getRating()); stmt.setInt(4, ti.getPoints()); stmt.setInt(5, ti.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[TournamentInscriptionService] update : " + e.getMessage()); }
        return false;
    }

    private TournamentInscription map(ResultSet rs) throws SQLException {
        return new TournamentInscription(
            rs.getInt("id"), rs.getInt("tournoi_id"), rs.getInt("player_id"),
            rs.getInt("rating"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getInt("points")
        );
    }
}
