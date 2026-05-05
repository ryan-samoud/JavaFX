package com.esports.controller;

import com.esports.model.Jeu;
import com.esports.model.User;
import com.esports.service.CategorieJeuService;
import com.esports.service.JeuService;
import com.esports.service.ReactionStatsService;
import com.esports.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StatisticsController implements Initializable {
    @FXML private Label lblKpiJeux;
    @FXML private Label lblKpiCats;
    @FXML private Label lblKpiNote;
    @FXML private Label lblKpiModes;
    @FXML private Label lblKpiUsers;
    @FXML private Label lblKpiReactions;

    @FXML private Canvas canvasPie;
    @FXML private Canvas canvasBar;
    @FXML private Canvas canvasLine;
    @FXML private Canvas canvasUsers;
    @FXML private Canvas canvasReactions;

    private final JeuService jeuService = new JeuService();
    private final CategorieJeuService categorieJeuService = new CategorieJeuService();
    private final UserService userService = new UserService();
    private final ReactionStatsService reactionStatsService = new ReactionStatsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Jeu> jeux = jeuService.findAll();
        List<User> users = userService.findAll();
        Map<String, Integer> byCategory = groupGamesByCategory(jeux);
        Map<String, Integer> byRole = groupUsersByRole(users);
        Map<String, Integer> reactionByType = reactionStatsService.countByType();

        updateKpis(jeux, users);
        drawPie(canvasPie, byCategory, "Jeux");
        drawBar(canvasBar, byCategory, "Volume");
        drawLineAvgNoteByCategory(canvasLine, jeux);
        drawBar(canvasUsers, byRole, "Users");
        drawPie(canvasReactions, reactionByType, "Reactions");
    }

    private void updateKpis(List<Jeu> jeux, List<User> users) {
        lblKpiJeux.setText(String.valueOf(jeux.size()));
        lblKpiCats.setText(String.valueOf(categorieJeuService.findAll().size()));
        double avg = jeux.stream().mapToDouble(Jeu::getNote).average().orElse(0.0);
        lblKpiNote.setText(String.format("%.2f", avg));
        long modeCount = jeux.stream().map(j -> safe(j.getMode(), "N/A").toLowerCase()).distinct().count();
        lblKpiModes.setText(String.valueOf(modeCount));
        lblKpiUsers.setText(String.valueOf(users.size()));
        lblKpiReactions.setText(String.valueOf(reactionStatsService.countAllReactions()));
    }

    private Map<String, Integer> groupGamesByCategory(List<Jeu> jeux) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Jeu j : jeux) {
            String key = (j.getCategorie() != null) ? safe(j.getCategorie().getNomCategorie(), "N/A") : "N/A";
            map.put(key, map.getOrDefault(key, 0) + 1);
        }
        return map;
    }

    private Map<String, Integer> groupUsersByRole(List<User> users) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (User u : users) {
            String role = safe(u.getRole(), "unknown").toLowerCase();
            map.put(role, map.getOrDefault(role, 0) + 1);
        }
        return map;
    }

    private void drawPie(Canvas canvas, Map<String, Integer> data, String centerText) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        clear(g, canvas);
        if (data.isEmpty()) {
            drawNoData(g, canvas);
            return;
        }

        List<Color> colors = palette();
        double total = data.values().stream().mapToDouble(Integer::doubleValue).sum();
        double start = 0;
        double size = Math.min(canvas.getWidth(), canvas.getHeight()) - 50;
        double x = (canvas.getWidth() - size) / 2;
        double y = (canvas.getHeight() - size) / 2;

        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            double angle = (e.getValue() / total) * 360.0;
            g.setFill(colors.get(i % colors.size()));
            g.fillArc(x, y, size, size, start, angle, javafx.scene.shape.ArcType.ROUND);
            start += angle;
            i++;
        }

        g.setFill(Color.web("#0f0d22"));
        g.fillOval(x + size * 0.28, y + size * 0.28, size * 0.44, size * 0.44);
        g.setFill(Color.web("#e9d5ff"));
        g.fillText(centerText, x + size * 0.43, y + size * 0.53);
    }

    private void drawBar(Canvas canvas, Map<String, Integer> data, String valueLabel) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        clear(g, canvas);
        if (data.isEmpty()) {
            drawNoData(g, canvas);
            return;
        }

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double padding = 35;
        int n = data.size();
        int max = data.values().stream().mapToInt(v -> v).max().orElse(1);
        double barW = (width - padding * 2) / Math.max(1, n * 1.4);
        int i = 0;
        List<Color> colors = palette();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            double h = (e.getValue() / (double) max) * (height - 80);
            double x = padding + i * (barW * 1.4);
            double y = height - 40 - h;

            g.setFill(colors.get(i % colors.size()));
            g.fillRoundRect(x, y, barW, h, 8, 8);
            g.setFill(Color.web("#9ca3af"));
            g.fillText(truncate(e.getKey(), 10), x, height - 18);
            g.fillText(String.valueOf(e.getValue()), x, y - 6);
            i++;
        }
        g.setFill(Color.web("#7c6fa8"));
        g.fillText(valueLabel, width - 60, 18);
    }

    private void drawLineAvgNoteByCategory(Canvas canvas, List<Jeu> jeux) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        clear(g, canvas);
        if (jeux.isEmpty()) {
            drawNoData(g, canvas);
            return;
        }

        Map<String, List<Jeu>> grouped = jeux.stream().collect(Collectors.groupingBy(j ->
                j.getCategorie() != null ? safe(j.getCategorie().getNomCategorie(), "N/A") : "N/A", LinkedHashMap::new, Collectors.toList()));
        List<String> labels = new ArrayList<>(grouped.keySet());
        List<Double> avgNotes = labels.stream()
                .map(k -> grouped.get(k).stream().mapToDouble(Jeu::getNote).average().orElse(0.0))
                .collect(Collectors.toList());

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double left = 40, bottom = h - 35, top = 20;
        double step = labels.size() > 1 ? (w - left - 20) / (labels.size() - 1) : 1;

        g.setStroke(Color.web("#38bdf8"));
        g.setLineWidth(2.2);
        for (int i = 0; i < avgNotes.size() - 1; i++) {
            double x1 = left + i * step;
            double y1 = bottom - (avgNotes.get(i) / 5.0) * (bottom - top);
            double x2 = left + (i + 1) * step;
            double y2 = bottom - (avgNotes.get(i + 1) / 5.0) * (bottom - top);
            g.strokeLine(x1, y1, x2, y2);
        }

        g.setFill(Color.web("#a5f3fc"));
        for (int i = 0; i < avgNotes.size(); i++) {
            double x = left + i * step;
            double y = bottom - (avgNotes.get(i) / 5.0) * (bottom - top);
            g.fillOval(x - 4, y - 4, 8, 8);
            g.fillText(String.format("%.1f", avgNotes.get(i)), x - 8, y - 10);
            g.setFill(Color.web("#9ca3af"));
            g.fillText(truncate(labels.get(i), 9), x - 12, bottom + 16);
            g.setFill(Color.web("#a5f3fc"));
        }
    }

    private void clear(GraphicsContext g, Canvas c) {
        g.setFill(Color.web("#0f0d22"));
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }

    private void drawNoData(GraphicsContext g, Canvas c) {
        g.setFill(Color.web("#9ca3af"));
        g.fillText("No data", c.getWidth() / 2 - 20, c.getHeight() / 2);
    }

    private List<Color> palette() {
        return List.of(
                Color.web("#a855f7"),
                Color.web("#ec4899"),
                Color.web("#38bdf8"),
                Color.web("#4ade80"),
                Color.web("#f59e0b"),
                Color.web("#f87171")
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
