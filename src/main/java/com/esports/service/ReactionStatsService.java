package com.esports.service;

import com.esports.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReactionStatsService {

    public int countAllReactions() {
        String sql = "SELECT COUNT(*) FROM jeu_reaction";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[ReactionStatsService] countAllReactions: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> countByType() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT type, COUNT(*) AS c FROM jeu_reaction GROUP BY type";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("type"), rs.getInt("c"));
            }
        } catch (Exception e) {
            System.err.println("[ReactionStatsService] countByType: " + e.getMessage());
        }
        return map;
    }
}
