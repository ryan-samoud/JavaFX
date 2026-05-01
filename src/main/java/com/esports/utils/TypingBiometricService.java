package com.esports.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypingBiometricService {

    public enum Result { NO_PROFILE, TOO_SHORT, MATCH, MISMATCH }

    // Storage format: "0.95,1.20,0.88,1.05;5"
    // comma-separated normalized intervals ; sample count

    // ── compare ───────────────────────────────────────────────────────
    public static Result compare(int userId, List<Double> captured) {
        if (captured.size() < 2) return Result.TOO_SHORT;

        List<Double> stored = loadIntervals(userId);
        if (stored == null) return Result.NO_PROFILE;
        if (stored.size() < 2) return Result.TOO_SHORT;

        double mae = alignedMAE(stored, captured);
        System.out.println("[Typing] MAE = " + String.format("%.4f", mae));
        return mae < 0.35 ? Result.MATCH : Result.MISMATCH;
    }

    // ── save (first time) ─────────────────────────────────────────────
    public static void saveProfile(int userId, List<Double> intervals) {
        if (intervals.size() < 2) return;
        String encoded = encode(intervals, 1);
        exec("UPDATE user SET typing_profile = ? WHERE id = ?", userId, encoded);
    }

    // ── update (rolling average after each login) ─────────────────────
    public static void updateProfile(int userId, List<Double> captured) {
        if (captured.size() < 2) return;
        List<Double> stored = loadIntervals(userId);
        int samples = loadSamples(userId);
        if (stored == null || stored.size() < 2) {
            saveProfile(userId, captured);
            return;
        }
        int len = Math.min(stored.size(), captured.size());
        List<Double> merged = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            // weight old profile heavily so it stays stable
            merged.add(stored.get(i) * 0.80 + captured.get(i) * 0.20);
        }
        exec("UPDATE user SET typing_profile = ? WHERE id = ?",
             userId, encode(merged, Math.min(samples + 1, 99)));
    }

    // ── enable / disable ─────────────────────────────────────────────
    public static boolean isEnabled(int userId) {
        String sql = "SELECT typing_biometric_enabled FROM user WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return rs.getInt(1) == 1;
        } catch (Exception e) {
            System.err.println("[Typing] isEnabled: " + e.getMessage());
        }
        return false;
    }

    public static void setEnabled(int userId, boolean enabled) {
        String sql = "UPDATE user SET typing_biometric_enabled = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, enabled ? 1 : 0);
            s.setInt(2, userId);
            s.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Typing] setEnabled: " + e.getMessage());
        }
    }

    public static void clearProfile(int userId) {
        String sql = "UPDATE user SET typing_profile = NULL, typing_biometric_enabled = 0 WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Typing] clearProfile: " + e.getMessage());
        }
    }

    public static void resetProfile(int userId) {
        String sql = "UPDATE user SET typing_profile = NULL WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Typing] resetProfile: " + e.getMessage());
        }
    }

    public static boolean hasProfile(int userId) {
        String raw = loadRaw(userId);
        return raw != null && !raw.isBlank();
    }

    // ── helpers ───────────────────────────────────────────────────────

    private static double alignedMAE(List<Double> a, List<Double> b) {
        int len = Math.min(a.size(), b.size());
        double sum = 0;
        for (int i = 0; i < len; i++) sum += Math.abs(a.get(i) - b.get(i));
        return sum / len;
    }

    private static String encode(List<Double> intervals, int samples) {
        String vals = intervals.stream()
                .map(v -> String.format("%.4f", v))
                .collect(Collectors.joining(","));
        return vals + ";" + samples;
    }

    private static List<Double> loadIntervals(int userId) {
        String raw = loadRaw(userId);
        if (raw == null || raw.isBlank()) return null;
        try {
            String[] parts = raw.split(";");
            return Arrays.stream(parts[0].split(","))
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        } catch (Exception e) { return null; }
    }

    private static int loadSamples(int userId) {
        String raw = loadRaw(userId);
        if (raw == null || !raw.contains(";")) return 1;
        try { return Integer.parseInt(raw.split(";")[1]); }
        catch (Exception e) { return 1; }
    }

    private static String loadRaw(int userId) {
        String sql = "SELECT typing_profile FROM user WHERE id = ?";
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, userId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) return rs.getString("typing_profile");
        } catch (Exception e) {
            System.err.println("[Typing] load: " + e.getMessage());
        }
        return null;
    }

    private static void exec(String sql, int userId, String value) {
        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, value);
            s.setInt(2, userId);
            s.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Typing] exec: " + e.getMessage());
        }
    }
}