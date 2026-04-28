package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.service.TournamentService;
import com.esports.model.Tournament;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * CONTROLLER — TournamentsPublicController.java
 * Vue front publique : liste des tournois en cards, accessible sans connexion.
 * Palette NexUS : violet #a855f7 / magenta #ec4899 / cyan #00b8ff / vert #4ade80
 */
public class TournamentsPublicController implements Initializable {

    // ── Stats bar ──────────────────────────────────────────────────
    @FXML private Label lblTotalCount;
    @FXML private Label lblOpenCount;
    @FXML private Label lblUpcomingCount;
    @FXML private Label lblInProgressCount;

    // ── Filtres ────────────────────────────────────────────────────
    @FXML private TextField  fieldSearch;
    @FXML private ComboBox<String> comboFilter;

    // ── Grilles de cards par statut ────────────────────────────────
    @FXML private FlowPane paneOpen;
    @FXML private FlowPane paneUpcoming;
    @FXML private FlowPane paneInProgress;
    @FXML private FlowPane paneFinished;

    @FXML private Label lblEmpty;

    // ── Data ───────────────────────────────────────────────────────
    private final ITournamentService dao = new TournamentService();
    private List<Tournament>    allTournaments;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Remplir le ComboBox filtres
        comboFilter.getItems().addAll("Tous les statuts", "OPEN", "UPCOMING", "IN_PROGRESS", "FINISHED");
        comboFilter.setValue("Tous les statuts");

        // Listener recherche en live
        fieldSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        // Charger les tournois depuis la BDD
        loadTournaments();
    }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT DONNÉES
    // ══════════════════════════════════════════════════════════════

    private void loadTournaments() {
        allTournaments = dao.findAll();
        updateStats(allTournaments);
        renderCards(allTournaments);
    }

    private void updateStats(List<Tournament> list) {
        lblTotalCount.setText(String.valueOf(list.size()));
        lblOpenCount.setText(String.valueOf(
                list.stream().filter(t -> "OPEN".equals(t.getStatus())).count()));
        lblUpcomingCount.setText(String.valueOf(
                list.stream().filter(t -> "UPCOMING".equals(t.getStatus())).count()));
        lblInProgressCount.setText(String.valueOf(
                list.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count()));
    }

    // ══════════════════════════════════════════════════════════════
    // FILTRE
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String query  = fieldSearch.getText().trim().toLowerCase();
        String status = comboFilter.getValue();

        List<Tournament> filtered = allTournaments.stream()
                .filter(t -> query.isEmpty()
                        || t.getTitle().toLowerCase().contains(query)
                        || t.getGame().toLowerCase().contains(query))
                .filter(t -> "Tous les statuts".equals(status) || status.equals(t.getStatus()))
                .collect(Collectors.toList());

        updateStats(filtered);
        renderCards(filtered);
    }

    // ══════════════════════════════════════════════════════════════
    // RENDU DES CARDS
    // ══════════════════════════════════════════════════════════════

    private void renderCards(List<Tournament> list) {

        paneOpen.getChildren().clear();
        paneUpcoming.getChildren().clear();
        paneInProgress.getChildren().clear();
        paneFinished.getChildren().clear();

        if (list.isEmpty()) {
            lblEmpty.setVisible(true);
            lblEmpty.setManaged(true);
            return;
        }

        lblEmpty.setVisible(false);
        lblEmpty.setManaged(false);

        for (Tournament t : list) {
            VBox card = buildCard(t);
            switch (t.getStatus()) {
                case "OPEN"        -> paneOpen.getChildren().add(card);
                case "UPCOMING"    -> paneUpcoming.getChildren().add(card);
                case "IN_PROGRESS" -> paneInProgress.getChildren().add(card);
                case "FINISHED"    -> paneFinished.getChildren().add(card);
                default            -> paneUpcoming.getChildren().add(card);
            }
        }
    }

    /**
     * Construit une card tournoi pour la vue publique.
     * Taille fixe 280×220 px, style gaming sombre.
     */
    private VBox buildCard(Tournament t) {

        // ── Couleurs selon statut ────────────────────────────────
        String accentColor  = accentColor(t.getStatus());
        String borderColor  = borderColor(t.getStatus());
        String badgeStyle   = badgeStyle(t.getStatus());
        String badgeText    = badgeText(t.getStatus());

        // ── CARD container ──────────────────────────────────────
        VBox card = new VBox(12);
        card.setPrefWidth(270);
        card.setPrefHeight(220);
        card.setPadding(new Insets(22, 22, 18, 22));
        card.setStyle(
                "-fx-background-color: rgba(17,11,40,0.92);" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-cursor: hand;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(26,14,55,0.98);" +
                "-fx-border-color: " + accentColor + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + accentColor + ", 18, 0.3, 0, 4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(17,11,40,0.92);" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-cursor: hand;"
        ));

        // ── Ligne 1 : badge statut + jeu emoji ──────────────────
        HBox topRow = new HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label badge = new Label(badgeText);
        badge.setStyle(badgeStyle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label gameEmoji = new Label(gameEmoji(t.getGame()));
        gameEmoji.setStyle("-fx-font-size: 20px;");

        topRow.getChildren().addAll(badge, spacer, gameEmoji);

        // ── Ligne 2 : titre ─────────────────────────────────────
        Label title = new Label(t.getTitle());
        title.setStyle(
                "-fx-text-fill: white; -fx-font-size: 15px;" +
                "-fx-font-weight: bold; -fx-wrap-text: true;"
        );
        title.setWrapText(true);
        title.setMaxWidth(230);

        // ── Ligne 3 : jeu ───────────────────────────────────────
        Label game = new Label(t.getGame());
        game.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        // ── Ligne 4 : prize pool ─────────────────────────────────
        HBox prizeRow = new HBox(6);
        prizeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label prizeIcon = new Label("🏆");
        prizeIcon.setStyle("-fx-font-size: 13px;");
        Label prize = new Label(String.format("%.0f €", t.getPrizePool()));
        prize.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");
        prizeRow.getChildren().addAll(prizeIcon, prize);

        // ── Ligne 5 : équipes + date ─────────────────────────────
        HBox infoRow = new HBox(16);
        infoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label teams = new Label("👥 " + t.getMaxTeams() + " équipes");
        teams.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        String dateStr = t.getStartDate() != null
                ? "📅 " + t.getStartDate().format(DATE_FMT)
                : "📅 Date TBD";
        Label date = new Label(dateStr);
        date.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        infoRow.getChildren().addAll(teams, date);

        // ── Bouton CTA ───────────────────────────────────────────
        Button cta = buildCtaButton(t.getStatus(), accentColor);

        // ── Assemblage ───────────────────────────────────────────
        Region spacerBot = new Region();
        VBox.setVgrow(spacerBot, Priority.ALWAYS);

        card.getChildren().addAll(topRow, title, game, prizeRow, infoRow, spacerBot, cta);

        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS STYLE
    // ══════════════════════════════════════════════════════════════

    private String accentColor(String status) {
        return switch (status) {
            case "OPEN"        -> "#4ade80";
            case "UPCOMING"    -> "#00b8ff";
            case "IN_PROGRESS" -> "#ec4899";
            default            -> "#4b5563";
        };
    }

    private String borderColor(String status) {
        return switch (status) {
            case "OPEN"        -> "rgba(74,222,128,0.25)";
            case "UPCOMING"    -> "rgba(0,184,255,0.25)";
            case "IN_PROGRESS" -> "rgba(236,72,153,0.25)";
            default            -> "rgba(75,85,99,0.25)";
        };
    }

    private String badgeText(String status) {
        return switch (status) {
            case "OPEN"        -> "● OUVERT";
            case "UPCOMING"    -> "◆ À VENIR";
            case "IN_PROGRESS" -> "▶ EN COURS";
            case "FINISHED"    -> "✓ TERMINÉ";
            default            -> status;
        };
    }

    private String badgeStyle(String status) {
        String color = accentColor(status);
        String bg    = switch (status) {
            case "OPEN"        -> "rgba(74,222,128,0.12)";
            case "UPCOMING"    -> "rgba(0,184,255,0.12)";
            case "IN_PROGRESS" -> "rgba(236,72,153,0.12)";
            default            -> "rgba(75,85,99,0.15)";
        };
        return "-fx-text-fill: " + color + "; -fx-background-color: " + bg + ";" +
               "-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;" +
               "-fx-padding: 4 10 4 10; -fx-background-radius: 20px;";
    }

    private String gameEmoji(String game) {
        if (game == null) return "🎮";
        String g = game.toLowerCase();
        if (g.contains("league") || g.contains("dota") || g.contains("moba")) return "⚔️";
        if (g.contains("cs") || g.contains("valorant") || g.contains("fps")) return "🔫";
        if (g.contains("fortnite") || g.contains("pubg") || g.contains("battle")) return "🪂";
        if (g.contains("fifa") || g.contains("foot")) return "⚽";
        if (g.contains("rocket")) return "🚀";
        return "🎮";
    }

    private Button buildCtaButton(String status, String accentColor) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);

        if ("FINISHED".equals(status)) {
            btn.setText("Voir les résultats");
            btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #6b7280;" +
                    "-fx-border-color: #374151; -fx-border-width: 1px;" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-font-size: 13px; -fx-padding: 9 0 9 0; -fx-cursor: hand;"
            );
        } else if ("IN_PROGRESS".equals(status)) {
            btn.setText("▶ Suivre en direct");
            btn.setStyle(
                    "-fx-background-color: rgba(236,72,153,0.15);" +
                    "-fx-text-fill: #ec4899;" +
                    "-fx-border-color: rgba(236,72,153,0.5); -fx-border-width: 1px;" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-padding: 9 0 9 0; -fx-cursor: hand;"
            );
        } else {
            btn.setText("S'inscrire au tournoi →");
            btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                    "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-padding: 9 0 9 0; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.4), 10, 0.2, 0, 2);"
            );
        }
        return btn;
    }

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/HomeView.fxml")
            );
            // Réutiliser la scène existante (préserve le contexte JVM et la session)
            javafx.scene.Scene scene = fieldSearch.getScene();
            scene.setRoot(root);
            Stage stage = (Stage) scene.getWindow();
            stage.setTitle("NexUS Gaming Arena");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
