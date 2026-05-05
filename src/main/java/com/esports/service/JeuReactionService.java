package com.esports.service;

import com.esports.model.JeuReaction;
import com.esports.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JeuReactionService {
    private static final Object LOCK = new Object();
    private static boolean schemaEnsured = false;

    private static void ensureSchema(Connection conn) throws SQLException {
        if (schemaEnsured) {
            return;
        }
        synchronized (LOCK) {
            if (schemaEnsured) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS jeu_reaction ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                                + "jeu_id INT NOT NULL, "
                                + "user_id INT NOT NULL, "
                                + "type VARCHAR(20) NOT NULL, "
                                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                                + "UNIQUE KEY uniq_jeu_user_type (jeu_id, user_id, type)"
                                + ")"
                );
            }
            schemaEnsured = true;
        }
    }

    // ── Generic toggle / check / count ──────────────────────────────────

    /**
     * Toggle a reaction of the given type. Returns true if the reaction is now
     * active, false if it was removed.
     */
    public boolean toggleReaction(int jeuId, int userId, String type) {
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureSchema(conn);
            if (hasReaction(jeuId, userId, type)) {
                removeReaction(jeuId, userId, type);
                return false;
            }
            addReaction(jeuId, userId, type);
            return true;
        } catch (Exception e) {
            System.err.println("[JeuReactionService] toggleReaction(" + type + "): " + e.getMessage());
            return false;
        }
    }

    public boolean hasReaction(int jeuId, int userId, String type) {
        String sql = "SELECT 1 FROM jeu_reaction WHERE jeu_id = ? AND user_id = ? AND type = ? LIMIT 1";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, jeuId);
                stmt.setInt(2, userId);
                stmt.setString(3, type);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            System.err.println("[JeuReactionService] hasReaction(" + type + "): " + e.getMessage());
            return false;
        }
    }

    public int countReactions(int jeuId, String type) {
        String sql = "SELECT COUNT(*) FROM jeu_reaction WHERE jeu_id = ? AND type = ?";
        try {
            Connection conn = DatabaseConnection.getInstance();
            ensureSchema(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, jeuId);
                stmt.setString(2, type);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JeuReactionService] countReactions(" + type + "): " + e.getMessage());
        }
        return 0;
    }

    private void addReaction(int jeuId, int userId, String type) throws Exception {
        String sql = "INSERT INTO jeu_reaction (jeu_id, user_id, type, created_at) VALUES (?, ?, ?, NOW())";
        Connection conn = DatabaseConnection.getInstance();
        ensureSchema(conn);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jeuId);
            stmt.setInt(2, userId);
            stmt.setString(3, type);
            stmt.executeUpdate();
        }
    }

    private void removeReaction(int jeuId, int userId, String type) throws Exception {
        String sql = "DELETE FROM jeu_reaction WHERE jeu_id = ? AND user_id = ? AND type = ?";
        Connection conn = DatabaseConnection.getInstance();
        ensureSchema(conn);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jeuId);
            stmt.setInt(2, userId);
            stmt.setString(3, type);
            stmt.executeUpdate();
        }
    }

    // ── Convenience shortcuts (Heart – kept for backward compat) ────────

    public boolean toggleHeart(int jeuId, int userId) {
        return toggleReaction(jeuId, userId, JeuReaction.TYPE_HEART);
    }

    public boolean isHearted(int jeuId, int userId) {
        return hasReaction(jeuId, userId, JeuReaction.TYPE_HEART);
    }

    public int countHearts(int jeuId) {
        return countReactions(jeuId, JeuReaction.TYPE_HEART);
    }

    // ── Like shortcuts ──────────────────────────────────────────────────

    public boolean toggleLike(int jeuId, int userId) {
        return toggleReaction(jeuId, userId, JeuReaction.TYPE_LIKE);
    }

    public boolean isLiked(int jeuId, int userId) {
        return hasReaction(jeuId, userId, JeuReaction.TYPE_LIKE);
    }

    public int countLikes(int jeuId) {
        return countReactions(jeuId, JeuReaction.TYPE_LIKE);
    }

    // ── Dislike shortcuts ───────────────────────────────────────────────

    public boolean toggleDislike(int jeuId, int userId) {
        return toggleReaction(jeuId, userId, JeuReaction.TYPE_DISLIKE);
    }

    public boolean isDisliked(int jeuId, int userId) {
        return hasReaction(jeuId, userId, JeuReaction.TYPE_DISLIKE);
    }

    public int countDislikes(int jeuId) {
        return countReactions(jeuId, JeuReaction.TYPE_DISLIKE);
    }

    // ── Favorite shortcuts ──────────────────────────────────────────────

    public boolean toggleFavorite(int jeuId, int userId) {
        return toggleReaction(jeuId, userId, JeuReaction.TYPE_FAVORITE);
    }

    public boolean isFavorited(int jeuId, int userId) {
        return hasReaction(jeuId, userId, JeuReaction.TYPE_FAVORITE);
    }

    public int countFavorites(int jeuId) {
        return countReactions(jeuId, JeuReaction.TYPE_FAVORITE);
    }
}
