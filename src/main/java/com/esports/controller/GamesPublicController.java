package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.model.User;
import com.esports.service.AuthService;
import com.esports.service.CategorieJeuService;
import com.esports.service.GeminiRecommendationService;
import com.esports.service.JeuReactionService;
import com.esports.service.JeuService;
import com.esports.service.JeuPdfService;
import com.esports.service.SpeechSearchService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;

public class GamesPublicController implements Initializable {

    @FXML private TextField fieldSearch;
    @FXML private FlowPane paneCategories;
    @FXML private FlowPane paneGames;
    @FXML private Label lblEmpty;
    @FXML private Label lblCategoryTitle;
    @FXML private TextArea areaChat;
    @FXML private TextField fieldChatPrompt;
    @FXML private Button btnSendPrompt;
    @FXML private Button btnVoiceSearch;
    @FXML private FlowPane paneAiRecommendations;
    @FXML private Label lblAiCardsTitle;
    @FXML private StackPane paneGameOfDayCard;
    @FXML private ImageView imgGameOfDay;
    @FXML private Label lblGameOfDayPlaceholder;
    @FXML private Label lblGameOfDayTitle;
    @FXML private Label lblGameOfDayMeta;
    @FXML private Label lblGameOfDayDesc;
    @FXML private Label lblGameOfDayLikes;
    @FXML private Label lblGameOfDayFavs;
    @FXML private Button btnGameOfDayPlay;

    private final CategorieJeuService categorieService = new CategorieJeuService();
    private final JeuService jeuService = new JeuService();
    private final JeuPdfService jeuPdfService = new JeuPdfService();
    private final JeuReactionService jeuReactionService = new JeuReactionService();
    private final GeminiRecommendationService geminiRecommendationService = new GeminiRecommendationService();
    private final SpeechSearchService speechSearchService = new SpeechSearchService();
    private SpeechSearchService.MicRecordingSession micSession;
    private boolean isRecordingVoice = false;

    private List<CategorieJeu> allCategories;
    private List<Jeu> allGames;
    private Integer selectedCategoryId = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
        renderGameOfTheDay();
        renderCategories();
        renderGames();

        fieldSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        fieldChatPrompt.setOnAction(e -> onAskRecommendation());
        areaChat.setText("NexUS AI ready.\nAsk me what kind of game you want (genre, mode, players, age, vibe...).");
        lblAiCardsTitle.setText("Recommandations IA");
        paneAiRecommendations.getChildren().clear();
    }

    private void loadData() {
        allCategories = categorieService.findAll();
        allGames = jeuService.findAll();
    }

    private void renderGameOfTheDay() {
        if (allGames == null || allGames.isEmpty()) {
            lblGameOfDayTitle.setText("No game available");
            lblGameOfDayMeta.setText("Add games to unlock spotlight");
            lblGameOfDayDesc.setText("The featured game appears automatically based on player reactions.");
            lblGameOfDayLikes.setText("👍 0 likes");
            lblGameOfDayFavs.setText("❤️ 0 favs");
            btnGameOfDayPlay.setDisable(true);
            return;
        }

        Jeu featured = allGames.stream()
                .max(Comparator.comparingInt((Jeu j) -> jeuReactionService.countLikes(j.getId()))
                        .thenComparingInt(j -> jeuReactionService.countHearts(j.getId()))
                        .thenComparingDouble(Jeu::getNote))
                .orElse(allGames.get(0));

        int likes = jeuReactionService.countLikes(featured.getId());
        int favs = jeuReactionService.countHearts(featured.getId());
        String category = featured.getCategorie() != null ? safe(featured.getCategorie().getNomCategorie(), "N/A") : "N/A";

        lblGameOfDayTitle.setText(safe(featured.getNom(), "Featured game"));
        lblGameOfDayMeta.setText("🎮 " + safe(featured.getMode(), "N/A") + "   •   📂 " + category + "   •   🔞 " + featured.getTrancheAge() + "+");
        lblGameOfDayDesc.setText(shortDescription(featured.getDescription()));
        lblGameOfDayLikes.setText("👍 " + likes + " likes");
        lblGameOfDayFavs.setText("❤️ " + favs + " favs");
        btnGameOfDayPlay.setDisable(false);
        btnGameOfDayPlay.setOnAction(e -> openGameDetails(featured));

        String imagePath = featured.getImage();
        if (imagePath != null && !imagePath.isBlank() && !"NULL".equalsIgnoreCase(imagePath)) {
            try {
                imgGameOfDay.setImage(new Image(imagePath, true));
                lblGameOfDayPlaceholder.setVisible(false);
                lblGameOfDayPlaceholder.setManaged(false);
            } catch (Exception e) {
                lblGameOfDayPlaceholder.setVisible(true);
                lblGameOfDayPlaceholder.setManaged(true);
            }
        } else {
            lblGameOfDayPlaceholder.setVisible(true);
            lblGameOfDayPlaceholder.setManaged(true);
        }

        applyGameOfTheDayAnimation();
    }

    private void applyGameOfTheDayAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(850), paneGameOfDayCard);
        fade.setFromValue(0.45);
        fade.setToValue(1.0);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(1700), paneGameOfDayCard);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.012);
        pulse.setToY(1.012);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);

        fade.play();
        pulse.play();
    }

    private void renderCategories() {
        paneCategories.getChildren().clear();

        // "All" Category
        VBox allCard = buildCategoryCard(null);
        paneCategories.getChildren().add(allCard);

        // "Favorites" Category (if logged in)
        if (AuthService.isLoggedIn()) {
            paneCategories.getChildren().add(buildFavoritesCard());
        }

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

    private VBox buildFavoritesCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefSize(120, 100);

        boolean isSelected = (selectedCategoryId != null && selectedCategoryId == -1);

        String baseStyle = "-fx-background-radius: 12; -fx-cursor: hand; -fx-transition: all 0.3s;";
        if (isSelected) {
            card.setStyle(baseStyle + "-fx-background-color: linear-gradient(to bottom right, #f43f5e, #ec4899); -fx-effect: dropshadow(gaussian, rgba(244,63,94,0.5), 10, 0, 0, 0);");
        } else {
            card.setStyle(baseStyle + "-fx-background-color: #1a1635; -fx-border-color: rgba(244,63,94,0.3); -fx-border-width: 1; -fx-border-radius: 12;");
        }

        Label icon = new Label("❤️");
        icon.setStyle("-fx-font-size: 24px;");

        Label name = new Label("Mes Favoris");
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        card.getChildren().addAll(icon, name);

        card.setOnMouseClicked(e -> {
            selectedCategoryId = -1;
            lblCategoryTitle.setText("Mes Jeux Favoris");
            renderCategories();
            applyFilters();
        });

        if (!isSelected) {
            card.setOnMouseEntered(e -> card.setStyle(baseStyle + "-fx-background-color: #251f4d; -fx-border-color: #f43f5e; -fx-border-width: 1; -fx-border-radius: 12;"));
            card.setOnMouseExited(e -> card.setStyle(baseStyle + "-fx-background-color: #1a1635; -fx-border-color: rgba(244,63,94,0.3); -fx-border-width: 1; -fx-border-radius: 12;"));
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

        // ── Reaction Bar: Like / Dislike / Favorite ────────────────────────
        VBox reactionBox = new VBox(6);
        reactionBox.setPadding(new Insets(8, 10, 8, 10));
        reactionBox.setStyle("-fx-background-color: rgba(15,12,36,0.72); -fx-border-color: rgba(168,85,247,0.28); -fx-border-radius: 12; -fx-background-radius: 12;");

        User current = AuthService.getCurrentUser();
        boolean isLoggedIn = current != null;

        // ── Row 1: Like + Dislike ───────────────────────────────────────
        HBox likeDislikeRow = new HBox(8);
        likeDislikeRow.setAlignment(Pos.CENTER_LEFT);

        // — Like —
        boolean liked = isLoggedIn && jeuReactionService.isLiked(j.getId(), current.getId());
        int likes = jeuReactionService.countLikes(j.getId());

        String likeActiveStyle = "-fx-background-color: linear-gradient(to right, rgba(34,197,94,0.35), rgba(16,185,129,0.30)); "
                + "-fx-text-fill: #bbf7d0; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(74,222,128,0.50); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";
        String likeInactiveStyle = "-fx-background-color: rgba(148,163,184,0.14); "
                + "-fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(203,213,225,0.22); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";

        Button btnLike = new Button(liked ? "👍 Liked" : "👍 Like");
        btnLike.setStyle(liked ? likeActiveStyle : likeInactiveStyle);
        Label lblLikeCount = new Label(String.valueOf(likes));
        lblLikeCount.setStyle("-fx-text-fill: #86efac; -fx-font-size: 11; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(34,197,94,0.18); -fx-background-radius: 999; -fx-padding: 2 7;");

        // — Dislike —
        boolean disliked = isLoggedIn && jeuReactionService.isDisliked(j.getId(), current.getId());
        int dislikes = jeuReactionService.countDislikes(j.getId());

        String dislikeActiveStyle = "-fx-background-color: linear-gradient(to right, rgba(239,68,68,0.35), rgba(249,115,22,0.28)); "
                + "-fx-text-fill: #fecaca; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(248,113,113,0.50); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";
        String dislikeInactiveStyle = "-fx-background-color: rgba(148,163,184,0.14); "
                + "-fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(203,213,225,0.22); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";

        Button btnDislike = new Button(disliked ? "👎 Disliked" : "👎 Dislike");
        btnDislike.setStyle(disliked ? dislikeActiveStyle : dislikeInactiveStyle);
        Label lblDislikeCount = new Label(String.valueOf(dislikes));
        lblDislikeCount.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(239,68,68,0.18); -fx-background-radius: 999; -fx-padding: 2 7;");

        Region ldSpacer = new Region();
        HBox.setHgrow(ldSpacer, Priority.ALWAYS);

        likeDislikeRow.getChildren().addAll(btnLike, lblLikeCount, ldSpacer, lblDislikeCount, btnDislike);

        // ── Row 2: Favorite (Heart) ─────────────────────────────────────
        HBox favoriteRow = new HBox(8);
        favoriteRow.setAlignment(Pos.CENTER_LEFT);

        boolean hearted = isLoggedIn && jeuReactionService.isHearted(j.getId(), current.getId());
        int hearts = jeuReactionService.countHearts(j.getId());

        String favActiveStyle = "-fx-background-color: linear-gradient(to right, rgba(239,68,68,0.32), rgba(244,114,182,0.32)); "
                + "-fx-text-fill: #ffe4e6; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(251,113,133,0.45); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";
        String favInactiveStyle = "-fx-background-color: rgba(148,163,184,0.14); "
                + "-fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: rgba(203,213,225,0.22); -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 5 12;";

        Label lblFavIcon = new Label("❤️");
        lblFavIcon.setStyle("-fx-font-size: 13;");
        Label lblFavTitle = new Label("Favorites");
        lblFavTitle.setStyle("-fx-text-fill: #fbcfe8; -fx-font-size: 11; -fx-font-weight: bold;");

        Button btnHeart = new Button(hearted ? "❤️ Favorited" : "🤍 Favorite");
        btnHeart.setStyle(hearted ? favActiveStyle : favInactiveStyle);
        Label lblHeartCount = new Label(String.valueOf(hearts));
        lblHeartCount.setStyle("-fx-text-fill: #fecdd3; -fx-font-size: 11; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(244,63,94,0.22); -fx-background-radius: 999; -fx-padding: 2 7;");

        Region heartSpacer = new Region();
        HBox.setHgrow(heartSpacer, Priority.ALWAYS);
        favoriteRow.getChildren().addAll(lblFavIcon, lblFavTitle, heartSpacer, lblHeartCount, btnHeart);

        reactionBox.getChildren().addAll(likeDislikeRow, favoriteRow);

        // ── Like button action ──────────────────────────────────────────
        btnLike.setOnAction(e -> {
            User user = AuthService.getCurrentUser();
            if (user == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connecte-toi pour liker.", ButtonType.OK).showAndWait();
                return;
            }
            // Toggle like
            boolean nowLiked = jeuReactionService.toggleLike(j.getId(), user.getId());
            // If we just liked, remove dislike if present
            if (nowLiked && jeuReactionService.isDisliked(j.getId(), user.getId())) {
                jeuReactionService.toggleDislike(j.getId(), user.getId());
                lblDislikeCount.setText(String.valueOf(jeuReactionService.countDislikes(j.getId())));
                btnDislike.setText("👎 Dislike");
                btnDislike.setStyle(dislikeInactiveStyle);
            }
            lblLikeCount.setText(String.valueOf(jeuReactionService.countLikes(j.getId())));
            btnLike.setText(nowLiked ? "👍 Liked" : "👍 Like");
            btnLike.setStyle(nowLiked ? likeActiveStyle : likeInactiveStyle);
        });

        // ── Dislike button action ───────────────────────────────────────
        btnDislike.setOnAction(e -> {
            User user = AuthService.getCurrentUser();
            if (user == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connecte-toi pour disliker.", ButtonType.OK).showAndWait();
                return;
            }
            // Toggle dislike
            boolean nowDisliked = jeuReactionService.toggleDislike(j.getId(), user.getId());
            // If we just disliked, remove like if present
            if (nowDisliked && jeuReactionService.isLiked(j.getId(), user.getId())) {
                jeuReactionService.toggleLike(j.getId(), user.getId());
                lblLikeCount.setText(String.valueOf(jeuReactionService.countLikes(j.getId())));
                btnLike.setText("👍 Like");
                btnLike.setStyle(likeInactiveStyle);
            }
            lblDislikeCount.setText(String.valueOf(jeuReactionService.countDislikes(j.getId())));
            btnDislike.setText(nowDisliked ? "👎 Disliked" : "👎 Dislike");
            btnDislike.setStyle(nowDisliked ? dislikeActiveStyle : dislikeInactiveStyle);
        });

        // ── Favorite (heart) button action ──────────────────────────────
        btnHeart.setOnAction(e -> {
            User user = AuthService.getCurrentUser();
            if (user == null) {
                new Alert(Alert.AlertType.INFORMATION, "Connecte-toi pour ajouter en favori.", ButtonType.OK).showAndWait();
                return;
            }
            boolean nowHearted = jeuReactionService.toggleHeart(j.getId(), user.getId());
            int updatedCount = jeuReactionService.countHearts(j.getId());
            lblHeartCount.setText(String.valueOf(updatedCount));
            btnHeart.setText(nowHearted ? "❤️ Favorited" : "🤍 Favorite");
            btnHeart.setStyle(nowHearted ? favActiveStyle : favInactiveStyle);
        });

        // Action buttons row
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);

        Button btnAction = new Button("🎮 Jouer Maintenant");
        btnAction.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        HBox.setHgrow(btnAction, Priority.ALWAYS);
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setOnAction(e -> openGameDetails(j));

        actionRow.getChildren().add(btnAction);

        content.getChildren().addAll(top, title, catLabel, players, reactionBox, actionRow);
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

    private void openGameDetails(Jeu jeu) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/GameDetailsView.fxml"));
            Parent root = loader.load();
            GameDetailsController controller = loader.getController();
            controller.setJeu(jeu);
            fieldSearch.getScene().setRoot(root);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la fiche jeu: " + ex.getMessage(), ButtonType.OK);
            err.setHeaderText("Erreur navigation");
            err.showAndWait();
        }
    }

    private void exportJeuPdf(Jeu jeu) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer la fiche jeu en PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("jeu-" + (jeu.getNom() != null ? jeu.getNom().replaceAll("[^a-zA-Z0-9-_]", "_") : jeu.getId()) + ".pdf");
            File out = chooser.showSaveDialog(fieldSearch.getScene().getWindow());
            if (out == null) {
                return;
            }

            jeuPdfService.exportJeuCardPdf(jeu, out);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "PDF exporte: " + out.getAbsolutePath(), ButtonType.OK);
            ok.setHeaderText("Export reussi");
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Echec export PDF: " + ex.getMessage(), ButtonType.OK);
            err.setHeaderText("Erreur");
            err.showAndWait();
        }
    }

    @FXML
    private void onAskRecommendation() {
        String prompt = fieldChatPrompt.getText() == null ? "" : fieldChatPrompt.getText().trim();
        if (prompt.isEmpty()) {
            return;
        }

        appendChat("You: " + prompt + "\n");
        fieldChatPrompt.clear();
        btnSendPrompt.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    return geminiRecommendationService.recommend(prompt, allGames);
                } catch (Exception e) {
                    return "AI error: " + e.getMessage();
                }
            }
        };

        task.setOnSucceeded(e -> {
            String aiResponse = task.getValue();
            List<Jeu> selected = selectDbOnlyRecommendations(prompt, aiResponse);
            appendChat("NexUS AI: " + buildDbOnlyResponseMessage(selected, aiResponse) + "\n");
            renderAiRecommendationCards(selected);
            btnSendPrompt.setDisable(false);
        });
        task.setOnFailed(e -> {
            appendChat("NexUS AI: Sorry, request failed.\n");
            btnSendPrompt.setDisable(false);
        });

        Thread thread = new Thread(task, "gemini-recommendation-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private List<Jeu> selectDbOnlyRecommendations(String prompt, String aiResponse) {
        List<Jeu> matched = extractGamesMentionedInResponse(aiResponse);
        if (matched.isEmpty()) {
            matched = fallbackRecommendations(prompt);
        }

        List<Jeu> unique = new ArrayList<>();
        for (Jeu jeu : matched) {
            if (unique.stream().noneMatch(x -> x.getId() == jeu.getId())) {
                unique.add(jeu);
            }
        }
        if (unique.size() > 6) {
            unique = unique.subList(0, 6);
        }
        return unique;
    }

    private void renderAiRecommendationCards(List<Jeu> unique) {
        paneAiRecommendations.getChildren().clear();

        if (unique.isEmpty()) {
            lblAiCardsTitle.setText("Recommandations IA - aucune correspondance");
            Label empty = new Label("Aucune recommandation exploitable pour le moment.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            paneAiRecommendations.getChildren().add(empty);
            return;
        }

        lblAiCardsTitle.setText("Recommandations IA (" + unique.size() + ")");
        for (Jeu jeu : unique) {
            paneAiRecommendations.getChildren().add(buildAiRecommendationCard(jeu));
        }
    }

    private String buildDbOnlyResponseMessage(List<Jeu> selected, String aiResponse) {
        if (selected == null || selected.isEmpty()) {
            return "I could not match a strong result. Try another query (genre/mode/category).";
        }
        String names = selected.stream()
                .map(j -> safe(j.getNom(), "Jeu"))
                .collect(Collectors.joining(", "));

        boolean usedFallback = aiResponse != null
                && aiResponse.toLowerCase().contains("gemini request failed (503)");
        if (usedFallback) {
            return "Gemini is busy now, so I used DB smart fallback: " + names;
        }
        return "From DB only, I recommend: " + names;
    }

    private List<Jeu> extractGamesMentionedInResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return List.of();
        }
        String lower = aiResponse.toLowerCase();
        return allGames.stream()
                .filter(j -> j.getNom() != null && !j.getNom().isBlank())
                .filter(j -> lower.contains(j.getNom().toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<Jeu> fallbackRecommendations(String prompt) {
        String q = prompt == null ? "" : prompt.toLowerCase();
        return allGames.stream()
                .filter(j -> {
                    String cat = (j.getCategorie() != null && j.getCategorie().getNomCategorie() != null)
                            ? j.getCategorie().getNomCategorie().toLowerCase() : "";
                    String mode = j.getMode() != null ? j.getMode().toLowerCase() : "";
                    String name = j.getNom() != null ? j.getNom().toLowerCase() : "";
                    String desc = j.getDescription() != null ? j.getDescription().toLowerCase() : "";
                    return q.isBlank() || name.contains(q) || desc.contains(q) || mode.contains(q) || cat.contains(q);
                })
                .sorted(Comparator.comparingDouble(Jeu::getNote).reversed())
                .limit(4)
                .collect(Collectors.toList());
    }

    private VBox buildAiRecommendationCard(Jeu j) {
        VBox card = new VBox(7);
        card.setPrefWidth(245);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(124,58,237,0.25), rgba(236,72,153,0.18));"
                + "-fx-background-radius: 12; -fx-border-color: rgba(196,181,253,0.35); -fx-border-radius: 12;");

        Label title = new Label(j.getNom() != null ? j.getNom() : "Jeu");
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-weight: bold;");

        String category = j.getCategorie() != null ? j.getCategorie().getNomCategorie() : "N/A";
        Label meta = new Label("📂 " + category + "   🎮 " + safe(j.getMode(), "N/A"));
        meta.setStyle("-fx-text-fill: #ddd6fe; -fx-font-size: 11;");

        Label stats = new Label("⭐ " + j.getNote() + "   👥 " + j.getNbJoueurs() + "   🔞 " + j.getTrancheAge() + "+");
        stats.setStyle("-fx-text-fill: #fde68a; -fx-font-size: 11; -fx-font-weight: bold;");

        Label reason = new Label(shortDescription(j.getDescription()));
        reason.setWrapText(true);
        reason.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 11;");

        Button open = new Button("Voir details");
        open.setStyle("-fx-background-color: rgba(59,130,246,0.28); -fx-text-fill: #dbeafe; -fx-font-weight: bold; -fx-background-radius: 8;");
        open.setOnAction(e -> openGameDetails(j));

        card.getChildren().addAll(title, meta, stats, reason, open);
        return card;
    }

    private String shortDescription(String text) {
        String s = safe(text, "Bon choix recommande par l'IA.");
        if (s.length() <= 95) return s;
        return s.substring(0, 92) + "...";
    }

    private void appendChat(String message) {
        if (areaChat.getText() == null || areaChat.getText().isBlank()) {
            areaChat.setText(message);
        } else {
            areaChat.appendText("\n" + message);
        }
        areaChat.positionCaret(areaChat.getText().length());
    }

    @FXML
    private void onVoiceSearch() {
        if (!isRecordingVoice) {
            try {
                micSession = speechSearchService.startMicrophoneRecording();
                isRecordingVoice = true;
                btnVoiceSearch.setText("⏹ Stop & Search");
                return;
            } catch (Exception startEx) {
                Alert err = new Alert(Alert.AlertType.ERROR,
                        "Cannot start microphone recording: " + startEx.getMessage(),
                        ButtonType.OK);
                err.setHeaderText("Voice Search Error");
                err.showAndWait();
                resetVoiceButtonState();
                return;
            }
        }

        File audio;
        try {
            audio = speechSearchService.stopMicrophoneRecording(micSession);
        } catch (Exception stopEx) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    "Cannot stop microphone recording: " + stopEx.getMessage(),
                    ButtonType.OK);
            err.setHeaderText("Voice Search Error");
            err.showAndWait();
            resetVoiceButtonState();
            return;
        }

        isRecordingVoice = false;
        micSession = null;
        btnVoiceSearch.setDisable(true);
        btnVoiceSearch.setText("Transcribing...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return speechSearchService.transcribeWithDeepgram(audio);
            }
        };

        task.setOnSucceeded(e -> {
            String transcript = task.getValue();
            fieldSearch.setText(transcript);
            applyFilters();
            resetVoiceButtonState();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Alert err = new Alert(Alert.AlertType.ERROR,
                    "Echec speech-to-text: " + (ex != null ? ex.getMessage() : "unknown error"),
                    ButtonType.OK);
            err.setHeaderText("Voice Search Error");
            err.showAndWait();
            resetVoiceButtonState();
        });

        Thread thread = new Thread(task, "deepgram-stt-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void resetVoiceButtonState() {
        isRecordingVoice = false;
        micSession = null;
        btnVoiceSearch.setDisable(false);
        btnVoiceSearch.setText("🎤 Voice Search");
    }

    private void applyFilters() {
        String query = fieldSearch.getText().toLowerCase().trim();
        List<Jeu> source = allGames;

        if (selectedCategoryId != null && selectedCategoryId == -1) {
            User current = AuthService.getCurrentUser();
            if (current != null) {
                source = jeuService.findFavoritesByUser(current.getId());
            } else {
                source = new ArrayList<>();
            }
        }

        List<Jeu> filtered = source.stream()
                .filter(j -> {
                    if (selectedCategoryId == null || selectedCategoryId == -1) return true;
                    return j.getCategorieId() == selectedCategoryId;
                })
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

    @FXML
    public void onShowFavorites() {
        if (!AuthService.isLoggedIn()) {
            new Alert(Alert.AlertType.INFORMATION, "Connectez-vous pour voir vos favoris.", ButtonType.OK).showAndWait();
            return;
        }
        selectedCategoryId = -1;
        lblCategoryTitle.setText("Mes Jeux Favoris");
        renderCategories();
        applyFilters();
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

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
