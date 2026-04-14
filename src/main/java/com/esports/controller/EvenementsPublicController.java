package com.esports.controller;

import com.esports.interfaces.IEvenementService;
import com.esports.model.Evenement;
import com.esports.service.EvenementService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * CONTROLLER — EvenementsPublicController.java
 * Vue front publique des événements — cards, filtres, tri dynamique.
 */
public class EvenementsPublicController implements Initializable {

    // Stats bar
    @FXML private Label lblTotalCount;
    @FXML private Label lblUpcomingCount;
    @FXML private Label lblPassedCount;

    // Filtres
    @FXML private TextField       fieldSearch;
    @FXML private ComboBox<String> comboFilter;
    @FXML private ComboBox<String> comboSort;

    // Grilles
    @FXML private FlowPane paneUpcoming;
    @FXML private FlowPane panePassed;
    @FXML private Label    lblEmpty;

    private final IEvenementService dao = new EvenementService();
    private List<Evenement> allEvents;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboFilter.getItems().addAll("Tous", "À venir", "Passés");
        comboFilter.setValue("Tous");

        comboSort.getItems().addAll("Date (récent)", "Date (ancien)", "Nom A→Z", "Participants ↓");
        comboSort.setValue("Date (récent)");

        fieldSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        comboFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        comboSort.valueProperty().addListener((obs, o, n) -> applyFilter());

        loadEvents();
    }

    // ══════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════

    private void loadEvents() {
        allEvents = dao.findAll();
        applyFilter();
    }

    private void updateStats(List<Evenement> list) {
        lblTotalCount.setText(String.valueOf(list.size()));
        lblUpcomingCount.setText(String.valueOf(list.stream().filter(e -> !e.isPast()).count()));
        lblPassedCount.setText(String.valueOf(list.stream().filter(Evenement::isPast).count()));
    }

    @FXML
    private void onSearch() { applyFilter(); }

    private void applyFilter() {
        String query  = fieldSearch.getText().trim().toLowerCase();
        String filter = comboFilter.getValue();
        String sort   = comboSort.getValue();

        List<Evenement> filtered = allEvents.stream()
                .filter(e -> query.isEmpty()
                        || e.getNom().toLowerCase().contains(query)
                        || e.getLieu().toLowerCase().contains(query))
                .filter(e -> switch (filter) {
                    case "À venir" -> !e.isPast();
                    case "Passés"  ->  e.isPast();
                    default        -> true;
                })
                .collect(Collectors.toList());

        // Tri
        if (sort != null) switch (sort) {
            case "Date (récent)"  -> filtered.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            case "Date (ancien)"  -> filtered.sort((a, b) -> a.getDate().compareTo(b.getDate()));
            case "Nom A→Z"        -> filtered.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Participants ↓" -> filtered.sort((a, b) -> Integer.compare(b.getNbrParticipant(), a.getNbrParticipant()));
        }

        updateStats(filtered);
        renderCards(filtered);
    }

    // ══════════════════════════════════════════════════
    // RENDU CARDS
    // ══════════════════════════════════════════════════

    private void renderCards(List<Evenement> list) {
        paneUpcoming.getChildren().clear();
        panePassed.getChildren().clear();

        if (list.isEmpty()) {
            lblEmpty.setVisible(true); lblEmpty.setManaged(true); return;
        }
        lblEmpty.setVisible(false); lblEmpty.setManaged(false);

        for (Evenement e : list) {
            VBox card = buildCard(e);
            if (e.isPast()) panePassed.getChildren().add(card);
            else            paneUpcoming.getChildren().add(card);
        }
    }

    private VBox buildCard(Evenement e) {
        String accent = e.isPast() ? "#4b5563" : "#a855f7";
        String border = e.isPast() ? "rgba(75,85,99,0.25)" : "rgba(168,85,247,0.25)";

        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setPrefHeight(230);
        card.setPadding(new Insets(22, 22, 18, 22));
        card.setStyle(
                "-fx-background-color: rgba(17,11,40,0.92);" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
                "-fx-background-radius: 14px; -fx-cursor: hand;"
        );

        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color: rgba(26,14,55,0.98);" +
                "-fx-border-color: " + accent + ";" +
                "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
                "-fx-background-radius: 14px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + accent + ", 18, 0.3, 0, 4);"
        ));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color: rgba(17,11,40,0.92);" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
                "-fx-background-radius: 14px; -fx-cursor: hand;"
        ));

        // Badge statut
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(e.isPast() ? "✓ PASSÉ" : "◆ À VENIR");
        badge.setStyle("-fx-text-fill: " + accent + "; -fx-background-color: " +
                (e.isPast() ? "rgba(75,85,99,0.15)" : "rgba(168,85,247,0.12)") + ";" +
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;" +
                "-fx-padding: 4 10 4 10; -fx-background-radius: 20px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label partLabel = new Label("👥 " + e.getNbrParticipant());
        partLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        topRow.getChildren().addAll(badge, spacer, partLabel);

        // Nom
        Label nom = new Label(e.getNom());
        nom.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        nom.setWrapText(true); nom.setMaxWidth(240);

        // Lieu
        Label lieu = new Label("📍 " + e.getLieu());
        lieu.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        lieu.setWrapText(true); lieu.setMaxWidth(240);

        // Date
        Label date = new Label("📅 " + (e.getDate() != null ? e.getDate().format(DATE_FMT) : "—"));
        date.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        // Description (tronquée)
        Label desc = new Label(e.getDescription() != null && e.getDescription().length() > 80
                ? e.getDescription().substring(0, 80) + "…" : e.getDescription() != null ? e.getDescription() : "");
        desc.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        desc.setWrapText(true); desc.setMaxWidth(240);

        Region bot = new Region(); VBox.setVgrow(bot, Priority.ALWAYS);

        // CTA
        Button cta = new Button(e.isPast() ? "Voir les détails" : "Participer →");
        cta.setMaxWidth(Double.MAX_VALUE);
        if (e.isPast()) {
            cta.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280;" +
                    "-fx-border-color: #374151; -fx-border-width: 1px;" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-font-size: 13px; -fx-padding: 9 0 9 0; -fx-cursor: hand;");
        } else {
            cta.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                    "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                    "-fx-padding: 9 0 9 0; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.4), 10, 0.2, 0, 2);");
        }

        card.getChildren().addAll(topRow, nom, lieu, date, desc, bot, cta);
        return card;
    }

    // ══════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════

    @FXML
    private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) fieldSearch.getScene().getWindow();
            double w = stage.getWidth(); double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setWidth(w); stage.setHeight(h);
            stage.setTitle("NexUS Gaming Arena");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
