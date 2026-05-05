package com.esports.controller;

import com.esports.model.Jeu;
import com.esports.service.GeminiRecommendationService;
import com.esports.service.JeuPdfService;
import com.esports.service.JeuService;
import com.esports.service.QrCodeService;
import com.esports.service.TwitchApiService;
import com.esports.service.YoutubeSearchService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GameDetailsController {
    @FXML private Label lblTitle;
    @FXML private Label lblCategory;
    @FXML private Label lblMode;
    @FXML private Label lblAge;
    @FXML private Label lblPlayers;
    @FXML private Label lblRating;
    @FXML private Label lblDescription;
    @FXML private Label lblStreamsState;
    @FXML private Label lblSimilarState;
    @FXML private VBox boxTwitchCards;
    @FXML private VBox boxYoutubeCards;
    @FXML private VBox boxSimilarGames;
    @FXML private Button btnSeeLiveTwitch;
    @FXML private Button btnRefreshSimilar;
    @FXML private ImageView imageGame;
    @FXML private ImageView imageQrCode;
    @FXML private Label lblQrHint;

    private final TwitchApiService twitchApiService = new TwitchApiService();
    private final YoutubeSearchService youtubeSearchService = new YoutubeSearchService();
    private final QrCodeService qrCodeService = new QrCodeService();
    private final GeminiRecommendationService geminiRecommendationService = new GeminiRecommendationService();
    private final JeuService jeuService = new JeuService();
    private final JeuPdfService jeuPdfService = new JeuPdfService();
    private Jeu jeu;

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
        if (jeu == null) {
            return;
        }
        lblTitle.setText(safe(jeu.getNom(), "Jeu"));
        lblCategory.setText(jeu.getCategorie() != null ? safe(jeu.getCategorie().getNomCategorie(), "N/A") : "N/A");
        lblMode.setText(safe(jeu.getMode(), "N/A"));
        lblAge.setText(jeu.getTrancheAge() + "+");
        lblPlayers.setText(String.valueOf(jeu.getNbJoueurs()));
        lblRating.setText(String.valueOf(jeu.getNote()));
        lblDescription.setText(safe(jeu.getDescription(), "Aucune description."));

        if (jeu.getImage() != null && !jeu.getImage().isBlank() && !"NULL".equalsIgnoreCase(jeu.getImage())) {
            try {
                imageGame.setImage(new Image(jeu.getImage(), true));
            } catch (Exception ignored) {
                // Keep placeholder image pane.
            }
        }

        // Generate big QR code for this game
        try {
            imageQrCode.setImage(qrCodeService.generateJeuQrImage(jeu, 200));
        } catch (Exception ignored) {
            // QR generation failed, leave empty
        }

        loadStreamingCards();
        loadSimilarGamesRecommendations();
    }

    private void loadStreamingCards() {
        lblStreamsState.setText("Chargement Twitch + YouTube...");
        boxTwitchCards.getChildren().clear();
        boxYoutubeCards.getChildren().clear();
        btnSeeLiveTwitch.setOnAction(e -> openTwitchSearchForGame());

        Task<List<TwitchApiService.TwitchStream>> twitchTask = new Task<>() {
            @Override
            protected List<TwitchApiService.TwitchStream> call() {
                return twitchApiService.getTopStreamsForGame(safe(jeu.getNom(), ""), 4);
            }
        };
        twitchTask.setOnSucceeded(e -> renderTwitchCards(twitchTask.getValue()));
        twitchTask.setOnFailed(e -> renderTwitchCards(List.of()));
        Thread t1 = new Thread(twitchTask, "twitch-cards-loader");
        t1.setDaemon(true);
        t1.start();

        Task<List<YoutubeSearchService.YoutubeVideo>> ytTask = new Task<>() {
            @Override
            protected List<YoutubeSearchService.YoutubeVideo> call() {
                return youtubeSearchService.searchVideos(safe(jeu.getNom(), ""), 4);
            }
        };
        ytTask.setOnSucceeded(e -> renderYoutubeCards(ytTask.getValue()));
        ytTask.setOnFailed(e -> renderYoutubeCards(List.of()));
        Thread t2 = new Thread(ytTask, "youtube-cards-loader");
        t2.setDaemon(true);
        t2.start();
    }

    private void renderTwitchCards(List<TwitchApiService.TwitchStream> streams) {
        boxTwitchCards.getChildren().clear();
        if (streams == null || streams.isEmpty()) {
            Label empty = new Label("Aucun stream Twitch dispo.");
            empty.setStyle("-fx-text-fill: #9ca3af;");
            boxTwitchCards.getChildren().add(empty);
            lblStreamsState.setText("Pas de stream via API. Utilise 'See Live on Twitch'.");
            return;
        }
        for (TwitchApiService.TwitchStream s : streams) {
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: rgba(168,85,247,0.25); -fx-border-radius: 10;");
            Label name = new Label("@" + s.getUserName());
            name.setStyle("-fx-text-fill: #e9d5ff; -fx-font-weight: bold;");
            Label title = new Label(s.getTitle());
            title.setWrapText(true);
            title.setStyle("-fx-text-fill: #e5e7eb;");
            Label stats = new Label("👁 " + s.getViewers() + "   🌐 " + s.getLanguage());
            stats.setStyle("-fx-text-fill: #a5b4fc;");
            Button open = new Button("Open Twitch");
            open.setStyle("-fx-background-color: rgba(168,85,247,0.25); -fx-text-fill: #ede9fe; -fx-font-weight: bold; -fx-background-radius: 8;");
            open.setOnAction(ev -> openExternal(s.getTwitchUrl()));
            card.getChildren().addAll(name, title, stats, open);
            boxTwitchCards.getChildren().add(card);
        }
        lblStreamsState.setText("Live content loaded.");
    }

    private void renderYoutubeCards(List<YoutubeSearchService.YoutubeVideo> videos) {
        boxYoutubeCards.getChildren().clear();
        if (videos == null || videos.isEmpty()) {
            Label empty = new Label("Aucune video YouTube trouvee.");
            empty.setStyle("-fx-text-fill: #9ca3af;");
            boxYoutubeCards.getChildren().add(empty);
            return;
        }
        for (YoutubeSearchService.YoutubeVideo v : videos) {
            HBox row = new HBox(8);
            row.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: rgba(220,38,38,0.25); -fx-border-radius: 10;");
            ImageView thumb = new ImageView(new Image(v.getThumbnailUrl(), true));
            thumb.setFitWidth(120);
            thumb.setFitHeight(68);
            thumb.setPreserveRatio(false);

            VBox info = new VBox(4);
            Label title = new Label(v.getTitle());
            title.setStyle("-fx-text-fill: #fee2e2; -fx-font-weight: bold;");
            Label id = new Label("ID: " + v.getVideoId());
            id.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11;");
            Button open = new Button("Open YouTube");
            open.setStyle("-fx-background-color: rgba(220,38,38,0.20); -fx-text-fill: #fee2e2; -fx-font-weight: bold; -fx-background-radius: 8;");
            open.setOnAction(ev -> openExternal(v.getUrl()));
            info.getChildren().addAll(title, id, open);
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(thumb, info, spacer);
            boxYoutubeCards.getChildren().add(row);
        }
    }

    private void loadSimilarGamesRecommendations() {
        if (jeu == null) {
            return;
        }
        lblSimilarState.setText("Gemini is generating similar games...");
        boxSimilarGames.getChildren().clear();
        btnRefreshSimilar.setDisable(true);
        btnRefreshSimilar.setOnAction(e -> loadSimilarGamesRecommendations());

        Task<List<Jeu>> task = new Task<>() {
            @Override
            protected List<Jeu> call() {
                List<Jeu> allGames = jeuService.findAll();
                List<Jeu> catalog = allGames.stream()
                        .filter(g -> g.getId() != jeu.getId())
                        .collect(Collectors.toList());
                if (catalog.isEmpty()) {
                    return List.of();
                }
                try {
                    String prompt = "Current game: " + safe(jeu.getNom(), "Game")
                            + ". Recommend 4 similar games from DB catalog only. "
                            + "Prioritize same category and mode when possible. "
                            + "Keep answer short and include exact game names.";
                    String aiResponse = geminiRecommendationService.recommend(prompt, catalog);
                    List<Jeu> matched = extractGamesMentionedInResponse(aiResponse, catalog);
                    if (matched.isEmpty()) {
                        matched = fallbackRecommendations(catalog);
                    }
                    return dedupeAndLimit(matched, 4);
                } catch (Exception ex) {
                    return dedupeAndLimit(fallbackRecommendations(catalog), 4);
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<Jeu> games = task.getValue();
            renderSimilarGames(games);
            btnRefreshSimilar.setDisable(false);
        });
        task.setOnFailed(e -> {
            renderSimilarGames(List.of());
            btnRefreshSimilar.setDisable(false);
        });

        Thread thread = new Thread(task, "gemini-similar-games-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderSimilarGames(List<Jeu> games) {
        boxSimilarGames.getChildren().clear();
        if (games == null || games.isEmpty()) {
            lblSimilarState.setText("No similar games available yet.");
            Label empty = new Label("Try adding more games to improve AI recommendations.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            boxSimilarGames.getChildren().add(empty);
            return;
        }
        lblSimilarState.setText("Gemini found " + games.size() + " similar games.");
        for (Jeu item : games) {
            boxSimilarGames.getChildren().add(buildSimilarGameCard(item));
        }
    }

    private VBox buildSimilarGameCard(Jeu game) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 10; "
                + "-fx-border-color: rgba(167,139,250,0.28); -fx-border-radius: 10;");

        Label title = new Label(safe(game.getNom(), "Jeu"));
        title.setStyle("-fx-text-fill: #f5f3ff; -fx-font-size: 13; -fx-font-weight: bold;");

        String category = game.getCategorie() != null ? safe(game.getCategorie().getNomCategorie(), "N/A") : "N/A";
        Label meta = new Label("📂 " + category + "   •   🎮 " + safe(game.getMode(), "N/A"));
        meta.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11;");

        Label stats = new Label("⭐ " + game.getNote() + "   •   👥 " + game.getNbJoueurs() + "   •   🔞 " + game.getTrancheAge() + "+");
        stats.setStyle("-fx-text-fill: #fde68a; -fx-font-size: 11; -fx-font-weight: bold;");

        Label desc = new Label(shortDescription(game.getDescription()));
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 11;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button open = new Button("Open this game");
        open.setStyle("-fx-background-color: rgba(59,130,246,0.22); -fx-text-fill: #dbeafe; "
                + "-fx-font-weight: bold; -fx-background-radius: 8;");
        open.setOnAction(e -> openRecommendedGameDetails(game));

        card.getChildren().addAll(title, meta, stats, desc, spacer, open);
        return card;
    }

    private void openRecommendedGameDetails(Jeu selected) {
        if (selected == null || selected.getId() == (jeu != null ? jeu.getId() : -1)) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/GameDetailsView.fxml"));
            Parent root = loader.load();
            GameDetailsController controller = loader.getController();
            controller.setJeu(selected);
            lblTitle.getScene().setRoot(root);
        } catch (Exception ex) {
            lblSimilarState.setText("Cannot open recommended game.");
        }
    }

    private List<Jeu> extractGamesMentionedInResponse(String aiResponse, List<Jeu> catalog) {
        if (aiResponse == null || aiResponse.isBlank() || catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        String lower = aiResponse.toLowerCase();
        return catalog.stream()
                .filter(j -> j.getNom() != null && !j.getNom().isBlank())
                .filter(j -> lower.contains(j.getNom().toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<Jeu> fallbackRecommendations(List<Jeu> catalog) {
        String targetCategory = jeu != null && jeu.getCategorie() != null ? safe(jeu.getCategorie().getNomCategorie(), "").toLowerCase() : "";
        String targetMode = jeu != null ? safe(jeu.getMode(), "").toLowerCase() : "";
        return catalog.stream()
                .sorted(Comparator
                        .comparingInt((Jeu j) -> similarityScore(j, targetCategory, targetMode))
                        .thenComparingDouble(Jeu::getNote)
                        .reversed())
                .limit(4)
                .collect(Collectors.toList());
    }

    private int similarityScore(Jeu candidate, String targetCategory, String targetMode) {
        int score = 0;
        String cat = candidate.getCategorie() != null ? safe(candidate.getCategorie().getNomCategorie(), "").toLowerCase() : "";
        String mode = safe(candidate.getMode(), "").toLowerCase();
        if (!targetCategory.isBlank() && targetCategory.equals(cat)) score += 3;
        if (!targetMode.isBlank() && targetMode.equals(mode)) score += 2;
        if (jeu != null && Math.abs(candidate.getTrancheAge() - jeu.getTrancheAge()) <= 3) score += 1;
        if (jeu != null && Math.abs(candidate.getNbJoueurs() - jeu.getNbJoueurs()) <= 2) score += 1;
        return score;
    }

    private List<Jeu> dedupeAndLimit(List<Jeu> games, int max) {
        if (games == null || games.isEmpty()) {
            return List.of();
        }
        List<Jeu> unique = new ArrayList<>();
        for (Jeu g : games) {
            if (unique.stream().noneMatch(x -> x.getId() == g.getId())) {
                unique.add(g);
            }
        }
        if (unique.size() > max) {
            return unique.subList(0, max);
        }
        return unique;
    }

    private String shortDescription(String text) {
        String s = safe(text, "Recommended game based on your current choice.");
        if (s.length() <= 95) return s;
        return s.substring(0, 92) + "...";
    }

    @FXML
    private void onBackToGames() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/GamesPublicView.fxml"));
            Parent root = loader.load();
            lblTitle.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onExportPdf() {
        if (jeu == null) return;
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter jeu en PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("jeu-" + (jeu.getNom() != null ? jeu.getNom().replaceAll("[^a-zA-Z0-9-_]", "_") : jeu.getId()) + ".pdf");
            File out = chooser.showSaveDialog(lblTitle.getScene().getWindow());
            if (out == null) {
                return;
            }
            jeuPdfService.exportJeuCardPdf(jeu, out);
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès: " + out.getAbsolutePath(), ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur export PDF: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void openExternal(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
        }
        Platform.runLater(() -> lblStreamsState.setText("Impossible d'ouvrir le navigateur pour: " + url));
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void openTwitchSearchForGame() {
        String gameName = safe(jeu != null ? jeu.getNom() : "", "game");
        String url = "https://www.twitch.tv/search?term=" + URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        openExternal(url);
    }
}
