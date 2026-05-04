package com.esports.service;

import com.esports.interfaces.IMatchService;
import com.esports.model.Match;
import com.esports.model.ParticipantRanking;
import com.esports.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class MatchService implements IMatchService {

    @Override
    public List<Match> findAll() {
        List<Match> list = new ArrayList<>();
        String sql = "SELECT * FROM match_tournoi ORDER BY date_match DESC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.err.println("[MatchService] findAll : " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Match> findByTournamentId(int tid) {
        List<Match> list = new ArrayList<>();
        String sql = "SELECT * FROM match_tournoi WHERE tournoi_id = ? ORDER BY date_match ASC";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { System.err.println("[MatchService] findByTournamentId : " + e.getMessage()); }
        return list;
    }

    @Override
    public boolean save(Match m) {
        String sql = "INSERT INTO match_tournoi (tournoi_id, date_match, round, statut, nom_joueur1, nom_joueur2, score_joueur1, score_joueur2) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, m.getTournoiId());
            stmt.setTimestamp(2, Timestamp.valueOf(m.getDateMatch()));
            stmt.setString(3, m.getRound());
            stmt.setString(4, m.getStatut());
            stmt.setString(5, m.getNomJoueur1());
            stmt.setString(6, m.getNomJoueur2());
            stmt.setInt(7, m.getScoreJoueur1());
            stmt.setInt(8, m.getScoreJoueur2());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) m.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) { System.err.println("[MatchService] save : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean update(Match m) {
        String sql = "UPDATE match_tournoi SET tournoi_id=?, date_match=?, round=?, statut=?, nom_joueur1=?, nom_joueur2=?, score_joueur1=?, score_joueur2=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, m.getTournoiId());
            stmt.setTimestamp(2, Timestamp.valueOf(m.getDateMatch()));
            stmt.setString(3, m.getRound());
            stmt.setString(4, m.getStatut());
            stmt.setString(5, m.getNomJoueur1());
            stmt.setString(6, m.getNomJoueur2());
            stmt.setInt(7, m.getScoreJoueur1());
            stmt.setInt(8, m.getScoreJoueur2());
            stmt.setInt(9, m.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[MatchService] update : " + e.getMessage()); }
        return false;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM match_tournoi WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("[MatchService] delete : " + e.getMessage()); }
        return false;
    }

    @Override
    public Match findById(int id) {
        String sql = "SELECT * FROM match_tournoi WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) { System.err.println("[MatchService] findById : " + e.getMessage()); }
        return null;
    }

    @Override
    public List<ParticipantRanking> getLeaderboard(int tid) {
        List<Match> matches = findByTournamentId(tid);
        Map<String, ParticipantRanking> stats = new HashMap<>();

        for (Match m : matches) {
            if (!"TERMINE".equals(m.getStatut())) continue;

            ParticipantRanking p1 = stats.computeIfAbsent(m.getNomJoueur1(), ParticipantRanking::new);
            ParticipantRanking p2 = stats.computeIfAbsent(m.getNomJoueur2(), ParticipantRanking::new);

            p1.addMatchPlayed(); p2.addMatchPlayed();
            p1.addGoalsFor(m.getScoreJoueur1()); p1.addGoalsAgainst(m.getScoreJoueur2());
            p2.addGoalsFor(m.getScoreJoueur2()); p2.addGoalsAgainst(m.getScoreJoueur1());

            if (m.getScoreJoueur1() > m.getScoreJoueur2()) {
                p1.addWin(); p2.addLoss();
            } else if (m.getScoreJoueur1() < m.getScoreJoueur2()) {
                p1.addLoss(); p2.addWin();
            } else {
                p1.addDraw(); p2.addDraw();
            }
        }

        List<ParticipantRanking> result = new ArrayList<>(stats.values());
        result.sort(Comparator.comparing(ParticipantRanking::getPoints)
                .thenComparing(ParticipantRanking::getGoalDifference)
                .reversed());
        return result;
    }

    private Match map(ResultSet rs) throws SQLException {
        return new Match(
                rs.getInt("id"),
                rs.getInt("tournoi_id"),
                rs.getTimestamp("date_match").toLocalDateTime(),
                rs.getString("round"),
                rs.getString("statut"),
                rs.getString("nom_joueur1"),
                rs.getString("nom_joueur2"),
                rs.getInt("score_joueur1"),
                rs.getInt("score_joueur2")
        );
    }
}
