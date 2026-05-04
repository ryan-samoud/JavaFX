package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.service.CategorieJeuService;
import com.esports.service.JeuService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CONTROLLER — StatisticsController.java
 * Affiche les KPI + graphiques (camembert, barres, courbe) pour les jeux & catégories.
 */
public class StatisticsController implements Initializable {

    // ── KPI labels ────────────────────────────────────────────────────────────
    @FXML private Label lblKpiJeux;
    @FXML private Label lblKpiCats;
    @FXML private Label lblKpiNote;
    @FXML private Label lblKpiModes;

    // ── Canvas charts ─────────────────────────────────────────────────────────
    @FXML private Canvas canvasPie;
    @FXML private Canvas canvasBar;
    @FXML private Canvas canvasLine;

    // ── Palette NexUS Gaming ──────────────────────────────────────────────────
    private static final Color[] PALETTE = {
        Color.web("#a855f7"), Color.web("#ec4899"), Color.web("#06b6d4"),
        Color.web("#22d3ee"), Color.web("#f59e0b"), Color.web("#10b981"),
        Color.web("#f43f5e"), Color.web("#8b5cf6"), Color.web("#14b8a6"),
        Color.web("#fb923c")
    };

    private final JeuService          jeuService  = new JeuService();
    private final CategorieJeuService catService  = new CategorieJeuService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Jeu>          jeux = jeuService.findAll();
        List<CategorieJeu> cats = catService.findAll();

        // ── KPI ───────────────────────────────────────────────────────────────
        lblKpiJeux.setText(String.valueOf(jeux.size()));
        lblKpiCats.setText(String.valueOf(cats.size()));

        // Moyenne des notes
        OptionalDouble avg = jeux.stream()
                .mapToDouble(Jeu::getNote)
                .filter(n -> n > 0)
                .average();
        lblKpiNote.setText(avg.isPresent()
                ? String.format("%.1f / 5", avg.getAsDouble()) : "—");

        // Modes distincts
        String modes = jeux.stream()
                .map(Jeu::getMode)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(" · "));
        lblKpiModes.setText(modes.isEmpty() ? "—" : modes);

        // ── Données par catégorie ─────────────────────────────────────────────
        // Map catId → nom
        Map<Integer, String> catNames = cats.stream()
                .collect(Collectors.toMap(CategorieJeu::getId, CategorieJeu::getNomCategorie));

        // Nombre de jeux par catégorie
        Map<String, Long> countByCat = jeux.stream()
                .collect(Collectors.groupingBy(
                        j -> catNames.getOrDefault(j.getCategorieId(), "Autre"),
                        Collectors.counting()
                ));

        // Note moyenne par catégorie
        Map<String, OptionalDouble> avgByCat = new LinkedHashMap<>();
        for (String catName : countByCat.keySet()) {
            OptionalDouble a = jeux.stream()
                    .filter(j -> catNames.getOrDefault(j.getCategorieId(), "Autre").equals(catName))
                    .mapToDouble(Jeu::getNote)
                    .filter(n -> n > 0)
                    .average();
            avgByCat.put(catName, a);
        }

        // ── Graphiques ────────────────────────────────────────────────────────
        drawPie(canvasPie, countByCat);
        drawBar(canvasBar, countByCat);
        drawLine(canvasLine, avgByCat);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PIE CHART
    // ═══════════════════════════════════════════════════════════════════════════
    private void drawPie(Canvas canvas, Map<String, Long> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Background
        gc.setFill(Color.web("#0f0d22"));
        gc.fillRect(0, 0, w, h);

        if (data.isEmpty()) { drawEmpty(gc, w, h); return; }

        long total = data.values().stream().mapToLong(Long::longValue).sum();
        double cx = w * 0.38, cy = h / 2.0, r = Math.min(cx, cy) - 20;

        double startAngle = 0;
        int i = 0;
        List<Map.Entry<String, Long>> entries = new ArrayList<>(data.entrySet());

        for (Map.Entry<String, Long> entry : entries) {
            double sweep = 360.0 * entry.getValue() / total;
            gc.setFill(PALETTE[i % PALETTE.length]);
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, startAngle, sweep, javafx.scene.shape.ArcType.ROUND);

            // Séparateur
            gc.setStroke(Color.web("#0a0818"));
            gc.setLineWidth(2);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, sweep, javafx.scene.shape.ArcType.ROUND);

            startAngle += sweep;
            i++;
        }

        // Trou central (donut)
        gc.setFill(Color.web("#0f0d22"));
        double innerR = r * 0.45;
        gc.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // Texte central
        gc.setFill(Color.web("#e2d9f3"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(total + " jeux", cx, cy - 4);
        gc.setFont(Font.font("System", 11));
        gc.setFill(Color.web("#7c6fa8"));
        gc.fillText("au total", cx, cy + 14);

        // Légende (droite)
        double legendX = w * 0.72, legendY = h * 0.12;
        double lineH = 22;
        gc.setFont(Font.font("System", 12));
        for (int j2 = 0; j2 < entries.size(); j2++) {
            Color col = PALETTE[j2 % PALETTE.length];
            String label = entries.get(j2).getKey();
            long count  = entries.get(j2).getValue();
            double pct  = 100.0 * count / total;

            gc.setFill(col);
            gc.fillRoundRect(legendX, legendY + j2 * lineH + 3, 12, 12, 4, 4);
            gc.setFill(Color.web("#c4b5fd"));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(truncate(label, 16) + "  " + count + "  (" + String.format("%.0f%%", pct) + ")",
                    legendX + 18, legendY + j2 * lineH + 14);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BAR CHART
    // ═══════════════════════════════════════════════════════════════════════════
    private void drawBar(Canvas canvas, Map<String, Long> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#0f0d22"));
        gc.fillRect(0, 0, w, h);

        if (data.isEmpty()) { drawEmpty(gc, w, h); return; }

        double padL = 50, padR = 20, padT = 30, padB = 55;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;
        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1);

        // Grille horizontale
        int gridLines = 5;
        gc.setStroke(Color.web("#1e1b4b"));
        gc.setLineWidth(1);
        gc.setFont(Font.font("System", 10));
        gc.setFill(Color.web("#7c6fa8"));
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int i = 0; i <= gridLines; i++) {
            double y = padT + chartH - (chartH * i / gridLines);
            gc.strokeLine(padL, y, padL + chartW, y);
            gc.fillText(String.valueOf((int)(maxVal * i / gridLines)), padL - 6, y + 4);
        }

        // Barres
        List<Map.Entry<String, Long>> entries = new ArrayList<>(data.entrySet());
        double barW = (chartW / entries.size()) * 0.6;
        double gap  = chartW / entries.size();

        for (int i = 0; i < entries.size(); i++) {
            long val = entries.get(i).getValue();
            double barH = chartH * val / maxVal;
            double bx = padL + i * gap + (gap - barW) / 2.0;
            double by = padT + chartH - barH;

            Color col = PALETTE[i % PALETTE.length];
            // Gradient visuel : rectangle principal + highlight
            gc.setFill(col.deriveColor(0, 1, 0.7, 1));
            gc.fillRoundRect(bx, by, barW, barH, 6, 6);
            gc.setFill(col.deriveColor(0, 0.8, 1.3, 0.6));
            gc.fillRoundRect(bx, by, barW * 0.4, barH, 6, 6);

            // Valeur au-dessus
            gc.setFill(Color.web("#e2d9f3"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(val), bx + barW / 2, by - 6);

            // Étiquette en bas (rotation simulée par texte court)
            gc.setFill(Color.web("#a78bfa"));
            gc.setFont(Font.font("System", 10));
            gc.fillText(truncate(entries.get(i).getKey(), 10), bx + barW / 2, padT + chartH + 16);
        }

        // Axe Y label
        gc.setFill(Color.web("#7c6fa8"));
        gc.setFont(Font.font("System", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Nombre de jeux", padL / 2.0, padT + chartH / 2.0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LINE CHART
    // ═══════════════════════════════════════════════════════════════════════════
    private void drawLine(Canvas canvas, Map<String, OptionalDouble> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#0f0d22"));
        gc.fillRect(0, 0, w, h);

        // Filtrer les catégories avec une note
        List<Map.Entry<String, OptionalDouble>> entries = data.entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toList());

        if (entries.isEmpty()) { drawEmpty(gc, w, h); return; }

        double padL = 55, padR = 20, padT = 30, padB = 55;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;
        double maxNote = 5.0;

        // Grille
        gc.setStroke(Color.web("#1e1b4b"));
        gc.setLineWidth(1);
        gc.setFont(Font.font("System", 10));
        gc.setFill(Color.web("#7c6fa8"));
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int i = 0; i <= 5; i++) {
            double y = padT + chartH - (chartH * i / 5);
            gc.strokeLine(padL, y, padL + chartW, y);
            gc.fillText(String.format("%.1f", maxNote * i / 5), padL - 6, y + 4);
        }

        // Ligne + points
        double step = chartW / (entries.size() - 1 == 0 ? 1 : entries.size() - 1);
        double[] xs = new double[entries.size()];
        double[] ys = new double[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            double note = entries.get(i).getValue().getAsDouble();
            xs[i] = padL + i * step;
            ys[i] = padT + chartH - (chartH * note / maxNote);
        }

        // Zone sous la courbe
        gc.setFill(Color.web("#a855f7", 0.15));
        gc.beginPath();
        gc.moveTo(xs[0], padT + chartH);
        for (int i = 0; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
        gc.lineTo(xs[xs.length - 1], padT + chartH);
        gc.closePath();
        gc.fill();

        // Courbe
        gc.setStroke(Color.web("#a855f7"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
        gc.stroke();

        // Points + labels
        for (int i = 0; i < entries.size(); i++) {
            double note = entries.get(i).getValue().getAsDouble();
            gc.setFill(Color.web("#ec4899"));
            gc.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
            gc.setFill(Color.web("#f0abfc"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.format("%.1f", note), xs[i], ys[i] - 10);

            // Étiquette bas
            gc.setFill(Color.web("#a78bfa"));
            gc.setFont(Font.font("System", 10));
            gc.fillText(truncate(entries.get(i).getKey(), 10), xs[i], padT + chartH + 16);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void drawEmpty(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.web("#7c6fa8"));
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Aucune donnée disponible", w / 2, h / 2);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
