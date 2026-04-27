package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.interfaces.ITournamentInscriptionService;
import com.esports.service.TournamentService;
import com.esports.service.TournamentInscriptionService;
import com.esports.service.AuthService;
import com.esports.model.Tournament;
import com.esports.model.TournamentInscription;
import com.esports.model.User;
import com.esports.interfaces.ITournamentService;
import com.esports.interfaces.ITournamentInscriptionService;
import com.esports.service.TicketService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.esports.service.LocalChatbotService;
import com.esports.service.GoogleCalendarService;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.Projection;
import com.sothawo.mapjfx.event.MarkerEvent;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.io.File;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.animation.AnimationTimer;
import javafx.stage.FileChooser;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import java.awt.Desktop;

/**
 * CONTROLLER — TournamentsPublicController.java
 * Vue front publique : liste des tournois en cards avec inscription intégrée.
 */
public class TournamentsPublicController implements Initializable {

    @FXML private Label lblTotalCount, lblOpenCount, lblUpcomingCount, lblInProgressCount, lblEmpty;
    @FXML private TextField fieldSearch;
    @FXML private ComboBox<String> comboFilter;
    @FXML private FlowPane paneOpen, paneUpcoming, paneInProgress, paneFinished;

    @FXML private VBox chatContainer;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";
    private static final String GEMINI_API_KEY = ""; // 🟢 Nouvelle clé API mise à jour (3)

    private final ITournamentService dao = new TournamentService();
    private final ITournamentInscriptionService inscriptionService = new TournamentInscriptionService();
    private final TicketService ticketService = new TicketService();
    private final AuthService authService = new AuthService();
    private final LocalChatbotService localBot = new LocalChatbotService();
    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();
    
    // Suppression de l'ancien MapService
    private List<Tournament> allTournaments;
    private YearMonth displayedMonth = YearMonth.now();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private enum Currency { TND, EUR, USD }
    private Currency currentCurrency = Currency.EUR;
    
    // Taux de conversion (base EUR)
    private static final double RATE_TND = 3.40;
    private static final double RATE_USD = 1.08;

    @FXML private Button btnTND, btnEUR, btnUSD;

    static {
        // Fix: Désactiver HTTP/2 pour le WebView afin d'éviter le crash NullPointerException dans HTTP2Loader
        System.setProperty("jdk.httpclient.HttpClient.version", "HTTP_1_1");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboFilter.getItems().addAll("Tous les statuts", "OUVERT", "DRAFT", "EN_COURS", "TERMINE");
        comboFilter.setValue("Tous les statuts");
        fieldSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        comboFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        loadTournaments();
    }

    private void loadTournaments() {
        allTournaments = dao.findAll();
        updateStats(allTournaments);
        renderCards(allTournaments);
    }

    private void updateStats(List<Tournament> list) {
        lblTotalCount.setText(String.valueOf(list.size()));
        lblOpenCount.setText(String.valueOf(list.stream().filter(t -> "OUVERT".equals(t.getStatut())).count()));
        lblUpcomingCount.setText(String.valueOf(list.stream().filter(t -> "DRAFT".equals(t.getStatut())).count()));
        lblInProgressCount.setText(String.valueOf(list.stream().filter(t -> "EN_COURS".equals(t.getStatut())).count()));
    }

    private void applyFilter() {
        String query = fieldSearch.getText().trim().toLowerCase();
        String status = comboFilter.getValue();
        List<Tournament> filtered = allTournaments.stream()
                .filter(t -> query.isEmpty() || t.getNom().toLowerCase().contains(query) || t.getJeu().toLowerCase().contains(query))
                .filter(t -> "Tous les statuts".equals(status) || status.equals(t.getStatut()))
                .collect(Collectors.toList());
        updateStats(filtered);
        renderCards(filtered);
    }

    @FXML private void onSearch() { applyFilter(); }

    private void renderCards(List<Tournament> list) {
        paneOpen.getChildren().clear(); paneUpcoming.getChildren().clear(); paneInProgress.getChildren().clear(); paneFinished.getChildren().clear();
        if (list.isEmpty()) { lblEmpty.setVisible(true); lblEmpty.setManaged(true); return; }
        lblEmpty.setVisible(false); lblEmpty.setManaged(false);
        for (Tournament t : list) {
            VBox card = buildCard(t);
            switch (t.getStatut()) {
                case "OUVERT" -> paneOpen.getChildren().add(card);
                case "DRAFT" -> paneUpcoming.getChildren().add(card);
                case "EN_COURS" -> paneInProgress.getChildren().add(card);
                case "TERMINE" -> paneFinished.getChildren().add(card);
                default -> paneUpcoming.getChildren().add(card);
            }
        }
    }

    private String formatPrice(double priceInEUR) {
        double converted = priceInEUR;
        String symbol = "€";
        
        switch (currentCurrency) {
            case TND:
                converted = priceInEUR * RATE_TND;
                symbol = " DT";
                break;
            case USD:
                converted = priceInEUR * RATE_USD;
                symbol = " $";
                break;
        }
        return String.format("%.0f%s", converted, symbol);
    }

    private VBox buildCard(Tournament t) {
        String accentColor = accentColor(t.getStatut());
        VBox card = new VBox(0); // Zero spacing because we use internal VBoxes
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #110f28; -fx-background-radius: 18; " +
                     "-fx-border-color: rgba(139, 92, 246, 0.15); -fx-border-width: 1.5; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 10);");

        // --- IMAGE HEADER ---
        StackPane imageHeader = new StackPane();
        imageHeader.setPrefHeight(140);
        imageHeader.setMinHeight(140);
        
        ImageView iv = new ImageView();
        iv.setFitWidth(280);
        iv.setFitHeight(140);
        iv.setPreserveRatio(false);
        
        // Clip rounded corners for the top of the image
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 140);
        clip.setArcWidth(36); clip.setArcHeight(36); // Match card radius
        iv.setClip(clip);

        try {
            String path = t.getImage();
            if (path != null && !path.isEmpty()) {
                String fullPath = path.startsWith("http") ? path : "file:/" + path.replace("\\", "/");
                iv.setImage(new Image(fullPath, true));
            } else {
                // Fallback style if no image
                imageHeader.setStyle("-fx-background-color: linear-gradient(to bottom right, #2e1065, #1e1b4b);");
            }
        } catch (Exception e) { 
            imageHeader.setStyle("-fx-background-color: #2a243d;");
        }

        Label badge = new Label(badgeText(t.getStatut()));
        badge.setStyle(badgeStyle(t.getStatut()));
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12));

        imageHeader.getChildren().addAll(iv, badge);

        // --- INFO BODY ---
        VBox body = new VBox(12);
        body.setPadding(new Insets(18));

        HBox topInfo = new HBox();
        topInfo.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(t.getNom().toUpperCase());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Arial Black';");
        
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblPrize = new Label("🏆 " + formatPrice(t.getPrize()));
        lblPrize.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-size: 12px;");
        topInfo.getChildren().addAll(title, sp, lblPrize);

        Label gameLab = new Label(t.getJeu());
        gameLab.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox statsRow = new HBox(12);
        Label lblDate = new Label("📅 " + (t.getDateDebut() != null ? t.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM")) : "TBD"));
        lblDate.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Label lblPart = new Label("👥 " + (t.getNbParticipantsActuels() != null ? t.getNbParticipantsActuels() : 0) + "/" + t.getNbParticipantsMax());
        lblPart.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Label lblLoc = new Label("📍 " + (t.getLocation() != null && !t.getLocation().isEmpty() ? t.getLocation() : "Online"));
        lblLoc.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");
        statsRow.getChildren().addAll(lblDate, lblPart, lblLoc);

        // Buttons Row
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER);
        
        Button btnAction = new Button("DÉTAILS & INSCRIPTION");
        btnAction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAction, Priority.ALWAYS);
        btnAction.setStyle("-fx-background-color: rgba(168, 85, 247, 0.1); -fx-text-fill: white; " +
                          "-fx-border-color: #a855f7; -fx-border-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 0;");

        // ── Bouton Google Calendar ──
        Button btnGCal = new Button("📅");
        btnGCal.setStyle("-fx-background-color: rgba(66, 133, 244, 0.12); -fx-text-fill: #93c5fd; " +
                         "-fx-border-color: #4285f4; -fx-border-radius: 8; -fx-font-size: 12px; -fx-padding: 7 10; -fx-cursor: hand;");
        Tooltip gcalTooltip = new Tooltip("Ajouter à Google Calendar");
        gcalTooltip.setStyle("-fx-font-size: 11px; -fx-background-color: #110f28; -fx-text-fill: white; -fx-border-color: #4285f4;");
        Tooltip.install(btnGCal, gcalTooltip);
        btnGCal.setOnAction(e -> addToGoogleCalendar(t));
        btnGCal.setOnMouseEntered(ev -> btnGCal.setStyle("-fx-background-color: rgba(66, 133, 244, 0.3); -fx-text-fill: white; " +
                         "-fx-border-color: #4285f4; -fx-border-radius: 8; -fx-font-size: 12px; -fx-padding: 7 10; -fx-cursor: hand;"));
        btnGCal.setOnMouseExited(ev -> btnGCal.setStyle("-fx-background-color: rgba(66, 133, 244, 0.12); -fx-text-fill: #93c5fd; " +
                         "-fx-border-color: #4285f4; -fx-border-radius: 8; -fx-font-size: 12px; -fx-padding: 7 10; -fx-cursor: hand;"));

        Button btnStream = new Button("🎥");
        btnStream.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #ef4444; " +
                           "-fx-border-color: #ef4444; -fx-border-radius: 8; -fx-font-size: 12px; -fx-padding: 7 10; -fx-cursor: hand;");
        Tooltip streamTooltip = new Tooltip("Diffuser en live");
        streamTooltip.setStyle("-fx-font-size: 11px; -fx-background-color: #110f28; -fx-text-fill: white; -fx-border-color: #ef4444;");
        Tooltip.install(btnStream, streamTooltip);
        btnStream.setOnAction(e -> onStartLive());

        actionsRow.getChildren().addAll(btnAction, btnGCal, btnStream);

        // Logic for action button color
        updateActionButton(btnAction, t);

        body.getChildren().addAll(topInfo, gameLab, statsRow, actionsRow);
        card.getChildren().addAll(imageHeader, body);

        // Hover Effect
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-8);
            card.setStyle("-fx-background-color: #1a1635; -fx-background-radius: 18; -fx-border-color: " + accentColor + "; -fx-border-width: 1.5;");
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setStyle("-fx-background-color: #110f28; -fx-background-radius: 18; -fx-border-color: rgba(139, 92, 246, 0.15); -fx-border-width: 1.5;");
        });

        return card;
    }

    private void updateActionButton(Button btn, Tournament t) {
        if (!"OUVERT".equals(t.getStatut())) {
            btn.setText("PLUS DISPONIBLE");
            btn.setDisable(true);
        } else {
            User user = AuthService.getCurrentUser();
            if (user == null) {
                btn.setText("SE CONNECTER");
                btn.setOnAction(e -> onBackHome());
            } else {
                List<TournamentInscription> userInsc = inscriptionService.findByPlayer(user.getId());
                boolean isInsc = userInsc.stream().anyMatch(i -> i.getTournoiId() == t.getId());
                if (isInsc) {
                    btn.setText("DÉJÀ INSCRIT (ANNULER)");
                    btn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: white; -fx-border-color: #ef4444; -fx-border-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 0;");
                    int insId = userInsc.stream().filter(u -> u.getTournoiId() == t.getId()).findFirst().get().getId();
                    btn.setOnAction(e -> handleUnregistration(insId));
                } else {
                    btn.setText("S'INSCRIRE →");
                    btn.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899); -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 9 0; -fx-background-radius: 8;");
                    btn.setOnAction(e -> handleRegistration(t));
                }
            }
        }
    }

    private void handleRegistration(Tournament t) {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        if (inscriptionService.register(new TournamentInscription(t.getId(), user.getId()))) {
            loadTournaments();
            String pdfPath = ticketService.generateBoardingPass(user, t);
            String successMsg = "Vous êtes inscrit au tournoi " + t.getNom() + " !";
            if (pdfPath != null) {
                successMsg += "\nVotre Boarding Pass a été généré :\n" + pdfPath;
                // Optionnel: Essayer d'ouvrir le PDF
                try {
                    java.io.File pdfFile = new java.io.File(pdfPath);
                    if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(pdfFile);
                    }
                } catch (Exception ex) { System.err.println("Impossible d'ouvrir le PDF: " + ex.getMessage()); }
            }
            showAlert("Succès", successMsg);
        }
    }

    private void handleUnregistration(int insId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annulation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment annuler votre inscription ?");
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            if (inscriptionService.unregister(insId)) {
                loadTournaments();
                showAlert("Succès", "Inscription annulée.");
            }
        }
    }

    @FXML private void onGoToMyInscriptions() {
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/com/esports/fxml/UserInscriptions.fxml"));
            fieldSearch.getScene().setRoot(r);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) fieldSearch.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onShowCalendar() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Calendrier des Tournois — Google Calendar");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0d0b1e;");

        // --- WebView avec Google Calendar interactif ---
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        // Intercepter les clics sur "Ajouter à Google Calendar" depuis le JS
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                // Injecter le bridge Java ↔ JavaScript
                netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new JavaBridge());
            }
        });

        // Intercepter la navigation vers des URLs Google Calendar
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && newUrl.contains("calendar.google.com/calendar/event")) {
                // Ouvrir dans le navigateur externe
                Platform.runLater(() -> {
                    try {
                        Desktop.getDesktop().browse(new URI(newUrl));
                        engine.loadContent(googleCalendarService.buildCalendarHtml(
                            allTournaments, displayedMonth.getYear(), displayedMonth.getMonthValue()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });

        // Charger le calendrier HTML avec FullCalendar
        String html = googleCalendarService.buildCalendarHtml(
                allTournaments, displayedMonth.getYear(), displayedMonth.getMonthValue());
        engine.loadContent(html);

        // --- Footer ---
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15));
        footer.setStyle("-fx-background-color: #110f28; -fx-border-color: rgba(139,92,246,0.15) transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Button btnAddAll = new Button("📅 AJOUTER TOUS LES TOURNOIS À GOOGLE CALENDAR");
        btnAddAll.setStyle("-fx-background-color: linear-gradient(to right, #4285f4, #34a853); " +
                           "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; " +
                           "-fx-padding: 10 25; -fx-background-radius: 8; -fx-cursor: hand;");
        btnAddAll.setOnMouseEntered(e -> btnAddAll.setStyle("-fx-background-color: linear-gradient(to right, #5a9cf6, #4fc363); " +
                           "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; " +
                           "-fx-padding: 10 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(66,133,244,0.5), 12, 0, 0, 4);"));
        btnAddAll.setOnMouseExited(e -> btnAddAll.setStyle("-fx-background-color: linear-gradient(to right, #4285f4, #34a853); " +
                           "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; " +
                           "-fx-padding: 10 25; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnAddAll.setOnAction(e -> {
            // Ouvrir le premier tournoi ouvert dans Google Calendar
            allTournaments.stream()
                .filter(t -> t.getDateDebut() != null && "OUVERT".equals(t.getStatut()))
                .findFirst()
                .ifPresentOrElse(
                    t -> addToGoogleCalendar(t),
                    () -> showAlert("Info", "Aucun tournoi ouvert avec date à ajouter.")
                );
        });

        Button btnClose = new Button("FERMER");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; " +
                          "-fx-border-color: #4b5563; -fx-border-radius: 6; -fx-padding: 10 25; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.close());

        footer.getChildren().addAll(btnAddAll, btnClose);
        root.getChildren().addAll(webView, footer);

        Scene scene = new Scene(root, 950, 700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Ouvre le navigateur pour ajouter un tournoi à Google Calendar.
     */
    private void addToGoogleCalendar(Tournament t) {
        try {
            String url = googleCalendarService.buildAddEventUrl(t);
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible d'ouvrir Google Calendar : " + ex.getMessage());
        }
    }

    /**
     * Bridge Java ↔ JavaScript pour le WebView.
     * Permet au JS d'appeler des méthodes Java.
     */
    public class JavaBridge {
        public void openUrl(String url) {
            Platform.runLater(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void onOpenGlobalMap() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("🗺️ Explorateur NexUS — Trouvez votre arène");

        MapView mapView = new MapView();
        mapView.setPrefSize(1100, 750);
        
        Map<String, Tournament> markerMap = new HashMap<>();

        if (allTournaments == null || allTournaments.isEmpty()) {
            allTournaments = dao.findAll();
        }

        // --- Marqueur de Pickpoint (Vert) ---
        Marker pickpointMarker;
        try {
            URL greenIcon = new URL("https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png");
            pickpointMarker = new Marker(greenIcon, -12, -41).setVisible(false);
        } catch (Exception e) { pickpointMarker = null; }
        
        final Marker fPickpoint = pickpointMarker;

        mapView.initializedProperty().addListener((obs, old, ready) -> {
            if (ready) {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                pause.setOnFinished(e -> {
                    mapView.setZoom(3);
                    mapView.setCenter(new Coordinate(36.8065, 10.1815));
                    
                    if (fPickpoint != null) mapView.addMarker(fPickpoint);

                    if (allTournaments != null) {
                        for (Tournament t : allTournaments) {
                            addMarker(mapView, t.getLocation(), Marker.Provided.RED, t, markerMap);
                        }
                    }
                });
                pause.play();
            }
        });

        VBox infoBox = new VBox(12);
        infoBox.setVisible(false);
        infoBox.setManaged(false);
        infoBox.setPadding(new Insets(20));
        infoBox.setMaxWidth(320);
        infoBox.setStyle("-fx-background-color: rgba(17, 15, 40, 0.95); -fx-border-color: #a855f7; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(168,85,247,0.4), 20, 0, 0, 8);");
        
        Label infoTitle = new Label();
        infoTitle.setWrapText(true);
        infoTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        
        Label infoDetails = new Label();
        infoDetails.setWrapText(true);
        infoDetails.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-line-spacing: 4px;");
        
        Button btnAction = new Button();
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899); -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 0; -fx-background-radius: 10;");

        infoBox.getChildren().addAll(infoTitle, infoDetails, btnAction);

        // --- GESTION DU CLIC SUR LA CARTE (PICKPOINT) ---
        mapView.addEventHandler(com.sothawo.mapjfx.event.MapViewEvent.MAP_CLICKED, event -> {
            Coordinate coord = event.getCoordinate();
            if (coord != null && fPickpoint != null) {
                fPickpoint.setPosition(coord);
                fPickpoint.setVisible(true);
                
                infoTitle.setText("🎯 POINT SÉLECTIONNÉ");
                infoTitle.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 15px;");
                infoDetails.setText(String.format("Lat: %.5f\nLon: %.5f\n\nVous pouvez utiliser ce point pour chercher des tournois à proximité.", 
                        coord.getLatitude(), coord.getLongitude()));
                btnAction.setText("TROUVER TOUT À 100KM");
                btnAction.setOnAction(e -> {
                    double pLat = coord.getLatitude();
                    double pLon = coord.getLongitude();
                    
                    List<Tournament> nearby = allTournaments.stream()
                        .filter(t -> {
                            String loc = t.getLocation();
                            if (loc == null || !loc.contains(",")) return false;
                            try {
                                String[] parts = loc.split("[,;\\s]+");
                                double tLat = Double.parseDouble(parts[0].replace(",", "."));
                                double tLon = Double.parseDouble(parts[1].replace(",", "."));
                                double dist = calculateDistance(pLat, pLon, tLat, tLon);
                                return dist <= 100; // Rayon de 100km
                            } catch (Exception ex) { return false; }
                        })
                        .toList();
                    
                    if (nearby.isEmpty()) {
                        showAlert("NexUS Discovery", "Aucun tournoi n'a été trouvé dans un rayon de 100km.");
                    } else {
                        stage.close();
                        displayTournaments(nearby); // Afficher uniquement les résultats proches
                        showAlert("NexUS Discovery", nearby.size() + " tournois trouvés à proximité !");
                    }
                });
                
                infoBox.setVisible(true);
                infoBox.setManaged(true);
            }
        });

        // --- GESTION DU CLIC SUR UN TOURNOI ---
        mapView.addEventHandler(MarkerEvent.MARKER_CLICKED, event -> {
            event.consume(); // Empêcher le clic de se propager au Pickpoint
            Tournament t = markerMap.get(event.getMarker().getId());
            if (t != null) {
                infoTitle.setText("🏆 " + t.getNom().toUpperCase());
                infoTitle.setStyle("-fx-text-fill: #60a5fa; -fx-font-weight: bold; -fx-font-size: 15px;");
                infoDetails.setText(String.format("🎮 %s\n💰 Prize: %.0f€\n📅 %s\n📍 %s", 
                        t.getJeu(), t.getPrize(), 
                        t.getDateDebut() != null ? t.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "TBD",
                        t.getLocation()));
                infoBox.setVisible(true);
                infoBox.setManaged(true);
                btnAction.setText("S'INSCRIRE →");
                btnAction.setOnAction(e -> { stage.close(); handleRegistration(t); });
            }
        });

        StackPane mapContainer = new StackPane(mapView, infoBox);
        StackPane.setAlignment(infoBox, Pos.TOP_RIGHT);
        StackPane.setMargin(infoBox, new Insets(25));

        stage.setScene(new Scene(mapContainer, 1100, 750));
        stage.show();
        mapView.initialize();
    }

    // --- LOGIQUE DE PROXIMITÉ GÉOGRAPHIQUE ---
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Rayon de la Terre en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void displayTournaments(List<Tournament> list) {
        paneOpen.getChildren().clear();
        paneUpcoming.getChildren().clear();
        paneInProgress.getChildren().clear();
        paneFinished.getChildren().clear();
        
        lblTotalCount.setText(String.valueOf(list.size()));
        
        for (Tournament t : list) {
            VBox card = buildCard(t);
            String status = (t.getStatut() != null) ? t.getStatut().toLowerCase() : "unknown";
            
            switch (status) {
                case "ouvert", "open" -> paneOpen.getChildren().add(card);
                case "upcoming", "a_venir" -> paneUpcoming.getChildren().add(card);
                case "in_progress", "en_cours" -> paneInProgress.getChildren().add(card);
                case "finished", "termine" -> paneFinished.getChildren().add(card);
            }
        }
        
        lblEmpty.setVisible(list.isEmpty());
        lblEmpty.setManaged(list.isEmpty());
    }

    private void addMarker(MapView mapView, String location, Marker.Provided color, Tournament data, Map<String, Tournament> markerMap) {
        if (location == null || location.isEmpty()) return;

        String[] parts = location.split("[,;\\s]+");
        if (parts.length >= 2) {
            try {
                double lat = Double.parseDouble(parts[0].trim().replace(",", "."));
                double lon = Double.parseDouble(parts[1].trim().replace(",", "."));
                Coordinate coord = new Coordinate(lat, lon);

                URL markerUrl = new URL("https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png");
                Marker marker = new Marker(markerUrl, -12, -41)
                        .setPosition(coord)
                        .setVisible(true);
                
                mapView.addMarker(marker);
                markerMap.put(marker.getId(), data);
            } catch (Exception e) {
                System.err.println("[MAP] Erreur format : " + location);
            }
        }
    }

    // --- CHATBOT LOGIC ---
    @FXML
    private void toggleChat() {
        boolean visible = chatContainer.isVisible();
        chatContainer.setVisible(!visible);
        chatContainer.setManaged(!visible);
        if (!visible) {
            chatArea.appendText("🤖 NexUS AI: Bonjour ! Je suis votre assistant NexUS. Comment puis-je vous aider aujourd'hui ?\n\n");
            chatInput.requestFocus();
        }
    }

    @FXML
    private void onSendMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        chatArea.appendText("👤 Vous: " + msg + "\n");
        chatInput.clear();

        // 1. TENTATIVE RÉPONSE LOCALE (BASE DE DONNÉES)
        String localResponse = localBot.getResponse(msg, authService.getCurrentUser());
        
        if (localResponse != null) {
            chatArea.appendText(localResponse + "\n\n");
            return;
        }

        // 2. FALLBACK VERS GEMINI SI PAS DE MOT-CLÉ LOCAL
        chatArea.appendText("🤖 NexUS AI: En train de réfléchir...\n");

        new Thread(() -> {
            String response = askGemini(msg);
            Platform.runLater(() -> {
                String currentText = chatArea.getText();
                int lastIdx = currentText.lastIndexOf("🤖 NexUS AI: En train de réfléchir...");
                if (lastIdx != -1) {
                    chatArea.setText(currentText.substring(0, lastIdx));
                }
                chatArea.appendText("🤖 NexUS AI: " + response + "\n\n");
            });
        }).start();
    }


    private String askGemini(String prompt) {
        if ("VOTRE_CLE_API_ICI".equals(GEMINI_API_KEY)) {
            return "Désolé, l'API Gemini n'est pas configurée.";
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Build Context from current tournament list
            String context = allTournaments.stream()
                .map(t -> String.format("- %s (%s), Prize: %.0f€, Statut: %s, Date: %s", 
                    t.getNom(), t.getJeu(), t.getPrize(), t.getStatut(), 
                    t.getDateDebut() != null ? t.getDateDebut().toString() : "TBD"))
                .collect(Collectors.joining("\n"));

            String systemPrompt = "Tu es l'assistant IA de NexUS Gaming Arena. Réponds brièvement en français.\n" +
                                "Voici les tournois actuels sur la plateforme :\n" + context + "\n\n" +
                                "Question de l'utilisateur : " + prompt;

            String jsonBody = new JSONObject()
                .put("contents", new JSONArray()
                    .put(new JSONObject()
                        .put("parts", new JSONArray()
                            .put(new JSONObject()
                                .put("text", systemPrompt)))))
                .toString();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + GEMINI_API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.getJSONArray("candidates")
                           .getJSONObject(0)
                           .getJSONObject("content")
                           .getJSONArray("parts")
                           .getJSONObject(0)
                           .getString("text");
            } else if (response.statusCode() == 404) {
                String models = listAvailableModels();
                return "Modèle non trouvé. Voici les modèles disponibles pour votre clé :\n" + models;
            } else {
                return "Erreur API (" + response.statusCode() + ") : " + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Une erreur est survenue lors de la communication avec l'IA.";
        }
    }

    private String listAvailableModels() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1/models?key=" + GEMINI_API_KEY))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray modelsArr = json.getJSONArray("models");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < modelsArr.length(); i++) {
                    sb.append("• ").append(modelsArr.getJSONObject(i).getString("name")).append("\n");
                }
                return sb.length() > 0 ? sb.toString() : "Aucun modèle de génération trouvé.";
            }
            return "Impossible de lister les modèles (" + response.statusCode() + ")";
        } catch (Exception e) { return "Erreur lors du listing : " + e.getMessage(); }
    }

    // --- CURRENCY LOGIC ---
    @FXML private void changeCurrencyTND() { updateCurrency(Currency.TND); }
    @FXML private void changeCurrencyEUR() { updateCurrency(Currency.EUR); }
    @FXML private void changeCurrencyUSD() { updateCurrency(Currency.USD); }

    private void updateCurrency(Currency newC) {
        this.currentCurrency = newC;
        
        // Update Buttons Style
        btnTND.setStyle(getCurrencyBtnStyle(newC == Currency.TND));
        btnEUR.setStyle(getCurrencyBtnStyle(newC == Currency.EUR));
        btnUSD.setStyle(getCurrencyBtnStyle(newC == Currency.USD));
        
        renderCards(allTournaments); // Refresh all cards with new prices
    }

    private String getCurrencyBtnStyle(boolean active) {
        return active 
            ? "-fx-background-color: #a855f7; -fx-text-fill: white; -fx-border-color: #a855f7; -fx-border-radius: 4; -fx-cursor: hand;"
            : "-fx-background-color: rgba(168,85,247,0.1); -fx-text-fill: white; -fx-border-color: #a855f7; -fx-border-radius: 4; -fx-cursor: hand;";
    }

    @FXML
    private void onStartLive() {
        String user = ""; // On demandera le pseudo
        
        TextInputDialog dialog = new TextInputDialog("pseudo_twitch");
        dialog.setTitle("NexUS Live Launcher");
        dialog.setHeaderText("🔴 LANCEMENT DU STREAMING");
        dialog.setContentText("Entrez votre pseudo Twitch :");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            String twitchPseudo = result.get().trim();
            if (twitchPseudo.isEmpty()) return;

            // --- LANCEMENT DU STUDIO NATIF NEXUS ---
            showNexUSStudio(twitchPseudo);
        }
    }

    private void showNexUSStudio(String pseudo) {
        Stage studio = new Stage();
        studio.setTitle("NexUS Broadcaster Studio - " + pseudo);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #0d0b1e; -fx-border-color: #a855f7; -fx-border-width: 2;");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("🔴 STUDIO DE DIFFUSION NEXUS");
        title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Arial Black';");

        // --- ZONE DE MEDIA (SCREEN + WEBCAM) ---
        StackPane mediaPane = new StackPane();
        mediaPane.setStyle("-fx-background-color: black; -fx-border-color: #a855f7; -fx-border-width: 3;");
        
        // Zone de Preview (Capture d'écran)
        ImageView screenPreview = new ImageView();
        screenPreview.setFitWidth(640);
        screenPreview.setFitHeight(360);
        screenPreview.setPreserveRatio(true);

        // Webcam (Overlay)
        WebView webcamView = new WebView();
        webcamView.setPrefSize(160, 120);
        webcamView.setMaxSize(160, 120);
        StackPane.setAlignment(webcamView, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(webcamView, new Insets(10));
        webcamView.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
        webcamView.getEngine().loadContent(
            "<html><body style='margin:0;padding:0;overflow:hidden;background:black;'>" +
            "<video id='v' autoplay playsinline style='width:100%;height:100%;object-fit:cover;'></video>" +
            "<script>navigator.mediaDevices.getUserMedia({video:true}).then(s=>{document.getElementById('v').srcObject=s;})</script>" +
            "</body></html>"
        );

        mediaPane.getChildren().addAll(screenPreview, webcamView);

        // --- VU-MÈTRE (MICROPHONE) ---
        ProgressBar micLevel = new ProgressBar(0);
        micLevel.setPrefWidth(640);
        micLevel.setStyle("-fx-accent: #22c55e;");
        Label lblMic = new Label("🎤 MICROPHONE ACTIVE");
        lblMic.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 10px;");
        VBox micBox = new VBox(5, lblMic, micLevel);

        // Moteur de capture
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            
            // Setup Audio Monitor
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            
            AnimationTimer timer = new AnimationTimer() {
                byte[] buffer = new byte[2000];
                @Override
                public void handle(long now) {
                    // Screen Capture
                    BufferedImage screen = robot.createScreenCapture(screenRect);
                    screenPreview.setImage(SwingFXUtils.toFXImage(screen, null));

                    // Mic Level (Simplified)
                    if (line.isOpen()) {
                        int read = line.read(buffer, 0, buffer.length);
                        double level = 0;
                        for (int i=0; i<read; i++) level += Math.abs(buffer[i]);
                        micLevel.setProgress(Math.min(1.0, level / 50000.0)); 
                    }
                }
            };
            
            Button btnStart = new Button("🚀 DÉMARRER LA DIFFUSION");
            btnStart.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-font-size: 16px; -fx-cursor: hand;");
            
            Button btnStop = new Button("🛑 ARRÊTER");
            btnStop.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-font-size: 16px; -fx-cursor: hand;");
            btnStop.setDisable(true);

            btnStart.setOnAction(e -> {
                try {
                    line.open(format);
                    line.start();
                    timer.start();
                    btnStart.setDisable(true);
                    btnStop.setDisable(false);
                    Desktop.getDesktop().browse(new URI("https://dashboard.twitch.tv/u/" + pseudo + "/stream-manager"));
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            btnStop.setOnAction(e -> {
                timer.stop();
                line.stop();
                line.close();
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                screenPreview.setImage(null);
                micLevel.setProgress(0);
            });

            HBox controls = new HBox(20, btnStart, btnStop);
            controls.setAlignment(Pos.CENTER);

            // --- BARRE DE PARTAGE SOCIAL ---
            Label lblShare = new Label("PARTAGER MON LIVE :");
            lblShare.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px; -fx-font-weight: bold;");
            
            String twitchUrl = "https://www.twitch.tv/" + pseudo;
            
            Button btnFb = new Button("Facebook");
            btnFb.setStyle("-fx-background-color: #1877f2; -fx-text-fill: white; -fx-cursor: hand;");
            btnFb.setOnAction(e -> share("https://www.facebook.com/sharer/sharer.php?u=" + twitchUrl));
            
            Button btnX = new Button("Twitter (X)");
            btnX.setStyle("-fx-background-color: #000000; -fx-text-fill: white; -fx-border-color: #4b5563; -fx-cursor: hand;");
            btnX.setOnAction(e -> share("https://twitter.com/intent/tweet?text=Je suis en direct sur NexUS !&url=" + twitchUrl));
            
            Button btnWa = new Button("WhatsApp");
            btnWa.setStyle("-fx-background-color: #25d366; -fx-text-fill: white; -fx-cursor: hand;");
            btnWa.setOnAction(e -> share("https://api.whatsapp.com/send?text=Venez voir mon live NexUS ! " + twitchUrl));
            
            Button btnCopy = new Button("📋 Copier le lien");
            btnCopy.setStyle("-fx-background-color: #4b5563; -fx-text-fill: white; -fx-cursor: hand;");
            btnCopy.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(twitchUrl);
                clipboard.setContent(content);
                btnCopy.setText("✅ Copié !");
            });

            HBox shareBar = new HBox(10, lblShare, btnFb, btnX, btnWa, btnCopy);
            shareBar.setAlignment(Pos.CENTER);
            shareBar.setPadding(new Insets(10, 0, 0, 0));

            root.getChildren().addAll(title, mediaPane, micBox, controls, shareBar);
            
            Scene scene = new Scene(root, 900, 700);
            studio.setScene(scene);
            studio.show();
            
            studio.setOnCloseRequest(e -> {
                timer.stop();
                if(line.isOpen()) line.close();
            });

        } catch (Exception ex) {
            showAlert("Erreur Studio", "Impossible d'initialiser les périphériques.");
        }
    }

    private void share(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    private String accentColor(String s) {
        return switch (s) {
            case "OUVERT" -> "#4ade80";
            case "DRAFT" -> "#00b8ff";
            case "EN_COURS" -> "#ec4899";
            default -> "#4b5563";
        };
    }

    private String badgeText(String s) {
        return switch (s) {
            case "OUVERT" -> "● OUVERT";
            case "DRAFT" -> "◆ À VENIR";
            case "EN_COURS" -> "▶ EN COURS";
            case "TERMINE" -> "✓ TERMINÉ";
            default -> s;
        };
    }

    private String badgeStyle(String s) {
        String c = accentColor(s);
        return "-fx-text-fill: " + c + "; -fx-background-color: rgba(255,255,255,0.05); -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 20;";
    }
}
