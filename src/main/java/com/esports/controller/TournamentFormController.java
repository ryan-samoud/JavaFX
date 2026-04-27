package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.model.Tournament;
import com.esports.service.TournamentService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.Projection;
import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TournamentFormController {

    @FXML private Label lblTitle;
    @FXML private TextField fieldNom;
    @FXML private TextField fieldJeu;
    @FXML private DatePicker pickerDebut;
    @FXML private TextField timeDebut;
    @FXML private DatePicker pickerFin;
    @FXML private TextField timeFin;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextField fieldNbMatchs;
    @FXML private TextField fieldPrize;
    @FXML private TextField fieldMaxPart;
    @FXML private TextField fieldImage;
    @FXML private TextField fieldLocation;
    @FXML private Button btnSave;

    @FXML private Label errNom;
    @FXML private Label errJeu;
    @FXML private Label errDates;
    @FXML private Label errNumeric;

    private final ITournamentService tournamentService = new TournamentService();
    private Tournament currentTournament;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean saved = false;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList("DRAFT", "OUVERT", "EN_COURS", "TERMINE"));
        comboStatut.setValue("DRAFT");
        pickerDebut.setValue(LocalDate.now());
        timeDebut.setText("09:00");
        pickerFin.setValue(LocalDate.now().plusDays(2));
        timeFin.setText("18:00");
        setupValidation();
    }

    private void setupValidation() {
        fieldNom.textProperty().addListener((o, old, n) -> {
            boolean tooShort = n.length() < 5;
            boolean exists = false;
            if (!tooShort) {
                if (currentTournament == null || currentTournament.getId() == 0) {
                    exists = tournamentService.existsByName(n);
                } else {
                    exists = tournamentService.existsByNameExcludeId(n, currentTournament.getId());
                }
            }
            
            if (tooShort) {
                errNom.setText("Min 5 caractères");
                errNom.setVisible(true);
            } else if (exists) {
                errNom.setText("Ce nom de tournoi existe déjà");
                errNom.setVisible(true);
            } else {
                errNom.setVisible(false);
            }
            
            fieldNom.setStyle((tooShort || exists) ? "-fx-border-color: #ff4757;" : "-fx-border-color: #3b2b5a;");
            validateAll();
        });
        fieldJeu.textProperty().addListener((o, old, n) -> {
            boolean invalid = n.length() < 5;
            errJeu.setVisible(invalid);
            fieldJeu.setStyle(invalid ? "-fx-border-color: #ff4757;" : "-fx-border-color: #3b2b5a;");
            validateAll();
        });
        pickerFin.valueProperty().addListener((o, old, n) -> validateDates());
        pickerDebut.valueProperty().addListener((o, old, n) -> validateDates());
        fieldNbMatchs.textProperty().addListener((o, old, n) -> validateNumeric());
        fieldPrize.textProperty().addListener((o, old, n) -> validateNumeric());
        fieldMaxPart.textProperty().addListener((o, old, n) -> validateNumeric());
    }

    private void validateDates() {
        boolean invalid = pickerFin.getValue() != null && pickerDebut.getValue() != null &&
                          pickerFin.getValue().isBefore(pickerDebut.getValue());
        errDates.setVisible(invalid);
        validateAll();
    }

    private void validateNumeric() {
        boolean invalid = !fieldNbMatchs.getText().matches("\\d*") ||
                          !fieldPrize.getText().matches("\\d*(\\.\\d+)?") ||
                          !fieldMaxPart.getText().matches("\\d*");
        errNumeric.setVisible(invalid);
        validateAll();
    }

    private void validateAll() {
        boolean nameExists;
        if (currentTournament == null || currentTournament.getId() == 0) {
            nameExists = tournamentService.existsByName(fieldNom.getText());
        } else {
            nameExists = tournamentService.existsByNameExcludeId(fieldNom.getText(), currentTournament.getId());
        }

        boolean isValid = fieldNom.getText().length() >= 5 &&
                          !nameExists &&
                          fieldJeu.getText().length() >= 5 &&
                          (pickerFin.getValue() == null || !pickerFin.getValue().isBefore(pickerDebut.getValue())) &&
                          fieldNbMatchs.getText().matches("\\d+") &&
                          fieldPrize.getText().matches("\\d+(\\.\\d+)?") &&
                          fieldMaxPart.getText().matches("\\d+");
        btnSave.setDisable(!isValid);
    }

    @FXML
    private void onBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(fieldNom.getScene().getWindow());
        if (selectedFile != null) fieldImage.setText(selectedFile.getAbsolutePath());
    }

    public void setTournament(Tournament t) {
        this.currentTournament = t;
        if (t != null) {
            lblTitle.setText("MODIFIER LE TOURNOI #" + t.getId());
            fieldNom.setText(t.getNom());
            fieldJeu.setText(t.getJeu());
            if (t.getDateDebut() != null) { pickerDebut.setValue(t.getDateDebut().toLocalDate()); timeDebut.setText(t.getDateDebut().toLocalTime().format(TIME_FMT)); }
            if (t.getDateFin() != null) { pickerFin.setValue(t.getDateFin().toLocalDate()); timeFin.setText(t.getDateFin().toLocalTime().format(TIME_FMT)); }
            comboStatut.setValue(t.getStatut());
            fieldNbMatchs.setText(String.valueOf(t.getNbMatchs()));
            fieldPrize.setText(String.valueOf(t.getPrize()));
            fieldMaxPart.setText(String.valueOf(t.getNbParticipantsMax()));
            fieldImage.setText(t.getImage());
            fieldLocation.setText(t.getLocation() != null ? t.getLocation() : "");
        }
        validateAll();
    }

    @FXML
    private void onSave() {
        LocalDateTime debut = LocalDateTime.of(pickerDebut.getValue(), LocalTime.parse(timeDebut.getText()));
        LocalDateTime fin = LocalDateTime.of(pickerFin.getValue(), LocalTime.parse(timeFin.getText()));
        if (currentTournament == null) currentTournament = new Tournament();
        currentTournament.setNom(fieldNom.getText());
        currentTournament.setJeu(fieldJeu.getText());
        currentTournament.setDateDebut(debut);
        currentTournament.setDateFin(fin);
        currentTournament.setStatut(comboStatut.getValue());
        currentTournament.setNbMatchs(Integer.parseInt(fieldNbMatchs.getText()));
        currentTournament.setPrize(Double.parseDouble(fieldPrize.getText()));
        currentTournament.setNbParticipantsMax(Integer.parseInt(fieldMaxPart.getText()));
        currentTournament.setImage(fieldImage.getText());
        currentTournament.setLocation(fieldLocation.getText());
        if (currentTournament.getId() == 0) { if (tournamentService.save(currentTournament)) { saved = true; close(); } }
        else { if (tournamentService.update(currentTournament)) { saved = true; close(); } }
    }

    @FXML private void onCancel() { close(); }

    @FXML
    private void onOpenMapPicker() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Sélectionner le lieu (Mapjfx Native)");

        MapView mapView = new MapView();
        
        // Attendre l'initialisation pour configurer la vue
        mapView.initializedProperty().addListener((obs, old, ready) -> {
            if (ready) {
                mapView.setZoom(6);
                mapView.setCenter(new Coordinate(36.8065, 10.1815));
            }
        });
        
        mapView.initialize();

        // Système de clic pour la sélection
        mapView.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            // Dans Mapjfx, on peut obtenir le point sous la souris
            // ou utiliser un marqueur. Ici on va capturer la position :
            // Pour plus de simplicité on utilise le coordonnée du clic
        });

        StackPane mapRoot = new StackPane(mapView);
        stage.setScene(new Scene(mapRoot, 850, 650));
        stage.show();
        mapView.initialize();

        // Ajout d'un marqueur de sélection (initialement invisible)
        try {
            URL markerUrl = new URL("https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png");
            Marker selectionMarker = new Marker(markerUrl, -12, -41).setVisible(false);
            
            mapView.initializedProperty().addListener((obs, old, ready) -> {
                if (ready) mapView.addMarker(selectionMarker);
            });

            mapView.addEventHandler(com.sothawo.mapjfx.event.MapViewEvent.MAP_CLICKED, event -> {
                Coordinate coord = event.getCoordinate();
                if (coord != null) {
                    selectionMarker.setPosition(coord);
                    selectionMarker.setVisible(true);
                    
                    // Petit délai pour voir le marqueur avant la fermeture
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.8));
                    pause.setOnFinished(e -> {
                        reverseGeocode(coord.getLatitude(), coord.getLongitude());
                        stage.close();
                    });
                    pause.play();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reverseGeocode(double lat, double lon) {
        String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lon);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "NexUS-App")
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        String address = json.optString("display_name", lat + ", " + lon);
                        Platform.runLater(() -> fieldLocation.setText(address));
                    } catch (Exception e) {
                        Platform.runLater(() -> fieldLocation.setText(lat + ", " + lon));
                    }
                });
    }

    private void close() { ((Stage) fieldNom.getScene().getWindow()).close(); }
    public boolean isSaved() { return saved; }
}
