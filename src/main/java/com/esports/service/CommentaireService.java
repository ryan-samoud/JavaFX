package com.esports.service;

import com.esports.interfaces.ICommentaireService;
import com.esports.model.Commentaire;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService implements ICommentaireService {

    public List<Commentaire> findByEvenement(int evenementId) {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT c.id, c.contenu, c.created_at, c.evenement_id, c.user_id, c.flagged, " +
                "CONCAT(u.prenom, ' ', u.nom) AS auteur_nom " +
                "FROM commentaire c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.evenement_id = ? " +
                "ORDER BY c.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, evenementId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[CommentaireService] findByEvenement ERROR: " + e.getMessage());
        }
        return list;
    }

    public List<Commentaire> findFlagged() {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT c.id, c.contenu, c.created_at, c.evenement_id, c.user_id, c.flagged, " +
                "CONCAT(u.prenom, ' ', u.nom) AS auteur_nom " +
                "FROM commentaire c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.flagged = 1 " +
                "ORDER BY c.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[CommentaireService] findFlagged ERROR: " + e.getMessage());
        }
        return list;
    }

    public boolean save(Commentaire c) {
        String sql = "INSERT INTO commentaire (contenu, evenement_id, user_id, flagged, created_at) VALUES (?,?,?,?,NOW())";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, c.getContenu());
            stmt.setInt(2, c.getEvenementId());
            stmt.setInt(3, c.getUserId());
            stmt.setBoolean(4, c.isFlagged());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) c.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[CommentaireService] save ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Commentaire c) {
        String sql = "UPDATE commentaire SET contenu=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, c.getContenu());
            stmt.setInt(2, c.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CommentaireService] update ERROR: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM commentaire WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CommentaireService] delete ERROR: " + e.getMessage());
        }
        return false;
    }

    public boolean approveFlagged(int id) {
        String sql = "UPDATE commentaire SET flagged=0 WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CommentaireService] approveFlagged ERROR: " + e.getMessage());
        }
        return false;
    }

    private Commentaire map(ResultSet rs) throws SQLException {
        Timestamp ca = rs.getTimestamp("created_at");
        return new Commentaire(
                rs.getInt("id"),
                rs.getString("contenu"),
                ca != null ? ca.toLocalDateTime() : java.time.LocalDateTime.now(),
                rs.getInt("evenement_id"),
                rs.getInt("user_id"),
                rs.getString("auteur_nom"),
                rs.getBoolean("flagged")
        );
    }
}
