package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.service.CategorieJeuService;
import com.esports.service.JeuService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GamesPublicController implements Initializable {

    @FXML private TextField fieldSearch;
    @FXML private FlowPane paneCategories;
    @FXML private FlowPane paneGames;
    @FXML private Label lblEmpty;
    @FXML private Label lblCategoryTitle;

    private final CategorieJeuService categorieService = new CategorieJeuService();
    private final JeuService jeuService = new JeuService();

    private List<CategorieJeu> allCategories;
    private List<Jeu> allGames;
    private Integer selectedCategoryId = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
        renderCategories();
        renderGames();

        fieldSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadData() {
        allCategories = categorieService.findAll();
        allGames = jeuService.findAll();
    }

    private void renderCategories() {
        paneCategories.getChildren().clear();

        // "All" Category
        VBox allCard = buildCategoryCard(null);
        paneCategories.getChildren().add(allCard);

        for (CategorieJeu c : allCategories) {
            paneCategories.getChildren().add(buildCategoryCard(c));
        }
    }

    private VBox buildCategoryCard(CategorieJeu c) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(120, 100);

        boolean isSelected = (c == null && selectedCategoryId == null) || (c != null && selectedCategoryId != null && c.getId() == selectedCategoryId);

        String baseStyle = "-fx-background-radius: 12; -fx-cursor: hand; -fx-transition: all 0.3s;";
        if (isSelected) {
            card.setStyle(baseStyle + "-fx-background-color: linear-gradient(to bottom right, #a855f7, #ec4899); -fx-effect: dropshadow(gaussian, rgba(168,85,247,0.5), 10, 0, 0, 0);");
        } else {
            card.setStyle(baseStyle + "-fx-background-color: #1a1635; -fx-border-color: rgba(168,85,247,0.3); -fx-border-width: 1; -fx-border-radius: 12;");
        }

        Label icon = new Label(c == null ? "🌐" : getCategoryEmoji(c.getGenre()));
        icon.setStyle("-fx-font-size: 24px;");

        Label name = new Label(c == null ? "Tous" : c.getNomCategorie());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        card.getChildren().addAll(icon, name);

        card.setOnMouseClicked(e -> {
            selectedCategoryId = (c == null) ? null : c.getId();
            lblCategoryTitle.setText(c == null ? "Tous les Jeux" : c.getNomCategorie());
            renderCategories();
            applyFilters();
        });

        if (!isSelected) {
            card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-background-color: #251f4d; -fx-border-color: #a855f7; -fx-border-width: 1; -fx-border-radius: 12;"));
            card.setOnMouseExited(e -> card.setStyle(baseStyle + "-fx-background-color: #1a1635; -fx-border-color: rgba(168,85,247,0.3); -fx-border-width: 1; -fx-border-radius: 12;"));
        }

        return card;
    }

    private void renderGames() {
        renderGames(allGames);
    }

    private void renderGames(List<Jeu> games) {
        paneGames.getChildren().clear();
        if (games.isEmpty()) {
            lblEmpty.setVisible(true);
            lblEmpty.setManaged(true);
        } else {
            lblEmpty.setVisible(false);
            lblEmpty.setManaged(false);
            for (Jeu j : games) {
                paneGames.getChildren().add(buildGameCard(j));
            }
        }
    }

    private VBox buildGameCard(Jeu j) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setPadding(new Insets(0)); // Zero padding to allow image to cover top
        card.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 15; -fx-border-color: rgba(168,85,247,0.2); -fx-border-width: 1; -fx-border-radius: 15; -fx-overflow: hidden;");

        // Image Container
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(160);
        imgContainer.setStyle("-fx-background-color: #251f4d; -fx-background-radius: 15 15 0 0;");
        
        Label placeholder = new Label("🎮");
        placeholder.setStyle("-fx-font-size: 40;");
        imgContainer.getChildren().add(placeholder);

        // Try to load image if path exists
        if (j.getImage() != null && !j.getImage().isEmpty() && !j.getImage().equals("NULL")) {
            try {
                // If it's a web URL or valid file path
                String path = j.getImage();
                if (path.startsWith("/uploads")) {
                   // Path from Symfony - might need adjustment based on where images are stored
                   // For now we just show the placeholder or try a local check
                } else {
                    ImageView iv = new ImageView(new Image(path, true));
                    iv.setFitWidth(280);
                    iv.setFitHeight(160);
                    iv.setPreserveRatio(false);
                    // Add clip for rounded corners
                    Rectangle clip = new Rectangle(280, 160);
                    clip.setArcWidth(30);
                    clip.setArcHeight(30);
                    iv.setClip(clip);
                    imgContainer.getChildren().add(iv);
                }
            } catch (Exception e) {
                // Ignore image load errors
            }
        }

        // Content Container
        VBox content = new VBox(8);
        content.setPadding(new Insets(12, 15, 15, 15));

        // Age and Mode row
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label ageBadge = new Label(j.getTrancheAge() + "+");
        ageBadge.setStyle("-fx-background-color: rgba(0,184,255,0.15); -fx-text-fill: #00b8ff; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");
        
        Label modeBadge = new Label(j.getMode());
        modeBadge.setStyle("-fx-background-color: rgba(74,222,128,0.15); -fx-text-fill: #4ade80; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label noteLabel = new Label("⭐ " + j.getNote());
        noteLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 12;");
        
        top.getChildren().addAll(ageBadge, modeBadge, spacer, noteLabel);

        Label title = new Label(j.getNom());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label catLabel = new Label((j.getCategorie() != null ? j.getCategorie().getNomCategorie() : "Jeux"));
        catLabel.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12; -fx-font-weight: bold;");

        Label players = new Label("👥 Max: " + j.getNbJoueurs() + " joueurs");
        players.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");

        Button btnAction = new Button("Jouer Maintenant");
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

        content.getChildren().addAll(top, title, catLabel, players, btnAction);
        card.getChildren().addAll(imgContainer, content);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #251f4d; -fx-background-radius: 15; -fx-border-color: #a855f7; -fx-border-width: 1; -fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(168,85,247,0.4), 20, 0, 0, 0);");
            card.setTranslateY(-8);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 15; -fx-border-color: rgba(168,85,247,0.2); -fx-border-width: 1; -fx-border-radius: 15;");
            card.setTranslateY(0);
        });

        return card;
    }

    private void applyFilters() {
        String query = fieldSearch.getText().toLowerCase().trim();
        List<Jeu> filtered = allGames.stream()
                .filter(j -> (selectedCategoryId == null || j.getCategorieId() == selectedCategoryId))
                .filter(j -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    String nom = j.getNom() != null ? j.getNom().toLowerCase() : "";
                    String desc = j.getDescription() != null ? j.getDescription().toLowerCase() : "";
                    return nom.contains(query) || desc.contains(query);
                })
                .collect(Collectors.toList());
        renderGames(filtered);
    }

    private String getCategoryEmoji(String genre) {
        if (genre == null) return "🎮";
        genre = genre.toLowerCase();
        if (genre.contains("action")) return "💥";
        if (genre.contains("strat")) return "🧠";
        if (genre.contains("sport")) return "⚽";
        if (genre.contains("fps")) return "🔫";
        if (genre.contains("rpg")) return "🧙";
        return "🎮";
    }

    @FXML
    private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            fieldSearch.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
