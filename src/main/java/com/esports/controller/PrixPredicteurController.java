package com.esports.controller;

import com.esports.service.PrixPredicteurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controller for PrixPredicteurView.fxml
 * Manages dynamic form rendering + AI price prediction logic.
 */
public class PrixPredicteurController implements Initializable {

    // ── FXML Injections ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbCategorie;
    @FXML private VBox formCard;
    @FXML private VBox dynamicFieldsContainer;
    @FXML private VBox resultCard;
    @FXML private VBox summaryCard;
    @FXML private VBox summaryContainer;
    @FXML private VBox infoCard;
    @FXML private Label lblPrixPredit;
    @FXML private Label lblUnite;
    @FXML private Label lblPrixMin;
    @FXML private Label lblPrixMax;
    @FXML private Button btnPredire;
    @FXML private Button btnAjouter;
    @FXML private Button btnRetour;

    // ── Service ────────────────────────────────────────────────────────────────
    private final PrixPredicteurService service = new PrixPredicteurService();

    // ── Dynamic widgets (populated per category) ───────────────────────────────
    // PC Gaming
    private ComboBox<String> cbGpu, cbCpu;
    private Slider slRam, slStockage;
    private ComboBox<String> cbTypeEcran;

    // Clavier
    private Slider slSwitchScore;
    private CheckBox chkRgbClavier, chkSansFilClavier;
    private Slider slTailleClavier;
    private ComboBox<String> cbMarqueClavier;

    // Souris
    private Slider slDpiScore;
    private CheckBox chkRgbSouris, chkSansFilSouris;
    private Slider slPoidsSouris;
    private ComboBox<String> cbMarqueSouris;

    // Casque
    private Slider slSonScore;
    private CheckBox chkMicroCasque, chkSansFilCasque, chkSurroundCasque;
    private ComboBox<String> cbMarqueCasque;

    // Écran
    private Slider slTailleEcran, slHzScore;
    private ComboBox<String> cbResolutionEcran, cbDalleEcran, cbMarqueEcran;

    // Chaise
    private Slider slConfortChaise, slMateriauChaise, slInclinaisonChaise;
    private CheckBox chkRgbChaise;
    private ComboBox<String> cbMarqueChaise;

    // Maillot
    private Slider slQualiteMaillot;
    private ComboBox<String> cbTailleMaillot, cbEditionMaillot, cbMarqueMaillot;

    // Tapis
    private Slider slTailleTapis, slEpaisseurTapis, slMateriauTapis;
    private CheckBox chkRgbTapis;

    // Manette
    private Slider slConfortManette, slCompatibiliteManette;
    private CheckBox chkSansFilManette, chkVibrationManette;
    private ComboBox<String> cbMarqueManette;

    // ── State ──────────────────────────────────────────────────────────────────
    private double dernierPrixPredit = 0;
    private String derniereCategorie = "";

    // ── Categories ─────────────────────────────────────────────────────────────
    private static final List<String> CATEGORIES = Arrays.asList(
            "PC Gaming", "Clavier", "Souris", "Casque", "Écran",
            "Chaise", "Maillot", "Tapis Souris", "Manette"
    );

    // ── CSS Helpers ────────────────────────────────────────────────────────────
    private static final String CARD_STYLE =
            "-fx-background-color: rgba(20,17,43,0.9); " +
                    "-fx-border-color: rgba(139,92,246,0.22); " +
                    "-fx-border-radius: 14; -fx-background-radius: 14; " +
                    "-fx-padding: 22 24 22 24;";
    private static final String LABEL_SECONDARY =
            "-fx-text-fill: #9ca3af; -fx-font-size: 12px;";
    private static final String LABEL_VALUE =
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String COMBO_STYLE =
            "-fx-background-color: rgba(139,92,246,0.1); " +
                    "-fx-border-color: rgba(139,92,246,0.4); " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 10 6 10;";
    private static final String SLIDER_STYLE =
            "-fx-control-inner-background: rgba(139,92,246,0.15); " +
                    "-fx-accent: #a855f7;";

    // ══════════════════════════════════════════════════════════════════════════
    //  Initialisation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbCategorie.getItems().addAll(CATEGORIES);
        cbCategorie.setStyle(COMBO_STYLE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Category change handler
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void onCategorieChanged() {
        String cat = cbCategorie.getValue();
        if (cat == null) return;
        derniereCategorie = cat;
        dynamicFieldsContainer.getChildren().clear();
        hideResults();

        switch (cat) {
            case "PC Gaming"    -> buildFormPcGaming();
            case "Clavier"      -> buildFormClavier();
            case "Souris"       -> buildFormSouris();
            case "Casque"       -> buildFormCasque();
            case "Écran"        -> buildFormEcran();
            case "Chaise"       -> buildFormChaise();
            case "Maillot"      -> buildFormMaillot();
            case "Tapis Souris" -> buildFormTapis();
            case "Manette"      -> buildFormManette();
        }

        formCard.setVisible(true);
        formCard.setManaged(true);
        infoCard.setVisible(false);
        infoCard.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Predict handler
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void onPredire() {
        try {
            double prix = 0;
            switch (derniereCategorie) {
                case "PC Gaming" -> {
                    String gpu = cbGpu.getValue() != null ? cbGpu.getValue() : "RTX 3060";
                    String cpu = cbCpu.getValue() != null ? cbCpu.getValue() : "i5-12400";
                    double ram = slRam.getValue();
                    double stockage = slStockage.getValue();
                    String typeEcran = cbTypeEcran.getValue() != null ? cbTypeEcran.getValue() : "IPS";
                    prix = service.predirePcGaming(gpu, ram, cpu, stockage, typeEcran);
                }
                case "Clavier" -> {
                    double switchScore = slSwitchScore.getValue();
                    double rgb = chkRgbClavier.isSelected() ? 1 : 0;
                    double sansFil = chkSansFilClavier.isSelected() ? 1 : 0;
                    double taille = slTailleClavier.getValue();
                    String marque = cbMarqueClavier.getValue() != null ? cbMarqueClavier.getValue() : "Logitech";
                    prix = service.predireClavier(switchScore, rgb, sansFil, taille, marque);
                }
                case "Souris" -> {
                    double dpiScore = slDpiScore.getValue();
                    double rgb = chkRgbSouris.isSelected() ? 1 : 0;
                    double sansFil = chkSansFilSouris.isSelected() ? 1 : 0;
                    double poids = slPoidsSouris.getValue();
                    String marque = cbMarqueSouris.getValue() != null ? cbMarqueSouris.getValue() : "Logitech";
                    prix = service.predireSouris(dpiScore, rgb, sansFil, poids, marque);
                }
                case "Casque" -> {
                    double sonScore = slSonScore.getValue();
                    double micro = chkMicroCasque.isSelected() ? 1 : 0;
                    double sansFil = chkSansFilCasque.isSelected() ? 1 : 0;
                    double surround = chkSurroundCasque.isSelected() ? 1 : 0;
                    String marque = cbMarqueCasque.getValue() != null ? cbMarqueCasque.getValue() : "Logitech";
                    prix = service.predireCasque(sonScore, micro, sansFil, surround, marque);
                }
                case "Écran" -> {
                    double tailleScore = slTailleEcran.getValue();
                    String resolution = cbResolutionEcran.getValue() != null ? cbResolutionEcran.getValue() : "1080p";
                    double hzScore = slHzScore.getValue();
                    String dalle = cbDalleEcran.getValue() != null ? cbDalleEcran.getValue() : "IPS";
                    String marque = cbMarqueEcran.getValue() != null ? cbMarqueEcran.getValue() : "ASUS";
                    prix = service.predireEcran(tailleScore, resolution, hzScore, dalle, marque);
                }
                case "Chaise" -> {
                    double confort = slConfortChaise.getValue();
                    double materiau = slMateriauChaise.getValue();
                    double rgb = chkRgbChaise.isSelected() ? 1 : 0;
                    double inclinaison = slInclinaisonChaise.getValue();
                    String marque = cbMarqueChaise.getValue() != null ? cbMarqueChaise.getValue() : "Secretlab";
                    prix = service.predireChaise(confort, materiau, rgb, inclinaison, marque);
                }
                case "Maillot" -> {
                    double qualite = slQualiteMaillot.getValue();
                    String taille = cbTailleMaillot.getValue() != null ? cbTailleMaillot.getValue() : "M";
                    String edition = cbEditionMaillot.getValue() != null ? cbEditionMaillot.getValue() : "Standard";
                    String marque = cbMarqueMaillot.getValue() != null ? cbMarqueMaillot.getValue() : "NoName";
                    prix = service.predireMaillot(qualite, taille, edition, marque);
                }
                case "Tapis Souris" -> {
                    double tailleScore = slTailleTapis.getValue();
                    double epaisseur = slEpaisseurTapis.getValue();
                    double rgb = chkRgbTapis.isSelected() ? 1 : 0;
                    double materiau = slMateriauTapis.getValue();
                    prix = service.predireTapis(tailleScore, epaisseur, rgb, materiau);
                }
                case "Manette" -> {
                    double confort = slConfortManette.getValue();
                    double sansFil = chkSansFilManette.isSelected() ? 1 : 0;
                    double vibration = chkVibrationManette.isSelected() ? 1 : 0;
                    double compatibilite = slCompatibiliteManette.getValue();
                    String marque = cbMarqueManette.getValue() != null ? cbMarqueManette.getValue() : "Logitech";
                    prix = service.predireManette(confort, sansFil, vibration, compatibilite, marque);
                }
            }

            afficherResultat(prix);
            buildSummary();

        } catch (Exception e) {
            showError("Erreur lors de la prédiction : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Navigation handlers
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void onRetour() {
        navigateTo("/com/esports/fxml/ShopView.fxml");
    }

    @FXML
    public void onAjouterBoutique() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esports/fxml/MainView.fxml"));
            Parent root = loader.load();

            // Récupérer le MainController et naviguer vers ShopView
            MainController mainController = loader.getController();
            mainController.onShop();

            // Afficher la fenêtre avec le sidebar
            javafx.stage.Stage stage = (javafx.stage.Stage) lblPrixPredit.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setTitle("Admin Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Navigation impossible : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Result display
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherResultat(double prix) {
        dernierPrixPredit = prix;
        double min = Math.round(prix * 0.85);
        double max = Math.round(prix * 1.15);

        lblPrixPredit.setText(String.format("%,.0f", prix));
        lblPrixMin.setText(String.format("%,.0f DT", min));
        lblPrixMax.setText(String.format("%,.0f DT", max));

        resultCard.setVisible(true);
        resultCard.setManaged(true);
        summaryCard.setVisible(true);
        summaryCard.setManaged(true);
    }

    private void hideResults() {
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        summaryCard.setVisible(false);
        summaryCard.setManaged(false);
    }

    private void buildSummary() {
        summaryContainer.getChildren().clear();
        addSummaryRow("Catégorie", derniereCategorie);
        addSummaryRow("Prix estimé", String.format("%,.0f DT", dernierPrixPredit));
        addSummaryRow("Fourchette", String.format("%,.0f – %,.0f DT",
                dernierPrixPredit * 0.85,
                dernierPrixPredit * 1.15));
    }

    private void addSummaryRow(String label, String value) {
        HBox row = new HBox();
        row.setStyle("-fx-alignment: center-left; -fx-spacing: 8;");
        Label lbl = new Label(label + " :");
        lbl.setStyle(LABEL_SECONDARY);
        lbl.setMinWidth(100);
        Label val = new Label(value);
        val.setStyle(LABEL_VALUE);
        row.getChildren().addAll(lbl, val);
        summaryContainer.getChildren().add(row);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Form builders — one per category
    // ══════════════════════════════════════════════════════════════════════════

    private void buildFormPcGaming() {
        cbGpu = makeComboBox("GPU", service.getGpuList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("GPU", cbGpu));

        cbCpu = makeComboBox("CPU", service.getCpuList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("CPU", cbCpu));

        slRam = makeSlider(8, 128, 16, 1, "Go");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("RAM", slRam, "Go"));

        slStockage = makeSlider(0.5, 8, 1, 0.5, "To");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Stockage", slStockage, "To"));

        cbTypeEcran = makeComboBox("Type écran", Arrays.asList("Non inclus", "FHD", "QHD", "4K OLED", "4K Mini-LED"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Écran inclus", cbTypeEcran));
    }

    private void buildFormClavier() {
        slSwitchScore = makeSlider(1, 10, 7, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Score switches", slSwitchScore, "/10"));

        chkRgbClavier = makeCheckBox("RGB");
        chkSansFilClavier = makeCheckBox("Sans fil");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRow(chkRgbClavier, chkSansFilClavier));

        slTailleClavier = makeSlider(1, 5, 3, 1, "");
        dynamicFieldsContainer.getChildren().add(
                makeSliderRowWithMap("Taille", slTailleClavier, Map.of(1, "40%", 2, "60%", 3, "TKL", 4, "100%", 5, "Gaming+")));

        cbMarqueClavier = makeComboBox("Marque", service.getMarqueList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueClavier));
    }

    private void buildFormSouris() {
        slDpiScore = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Score DPI", slDpiScore, "/10"));

        chkRgbSouris = makeCheckBox("RGB");
        chkSansFilSouris = makeCheckBox("Sans fil");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRow(chkRgbSouris, chkSansFilSouris));

        slPoidsSouris = makeSlider(1, 5, 3, 1, "");
        dynamicFieldsContainer.getChildren().add(
                makeSliderRowWithMap("Poids", slPoidsSouris, Map.of(1, "Ultra-léger", 2, "Léger", 3, "Moyen", 4, "Lourd", 5, "Très lourd")));

        cbMarqueSouris = makeComboBox("Marque", service.getMarqueList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueSouris));
    }

    private void buildFormCasque() {
        slSonScore = makeSlider(1, 10, 7, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Qualité son", slSonScore, "/10"));

        chkMicroCasque = makeCheckBox("Microphone intégré");
        chkSansFilCasque = makeCheckBox("Sans fil");
        chkSurroundCasque = makeCheckBox("Surround 7.1");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRow(chkMicroCasque, chkSansFilCasque));
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRowSingle(chkSurroundCasque));

        cbMarqueCasque = makeComboBox("Marque", service.getMarqueList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueCasque));
    }

    private void buildFormEcran() {
        slTailleEcran = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Score taille", slTailleEcran, "/10"));

        cbResolutionEcran = makeComboBox("Résolution", Arrays.asList("1080p", "1440p", "4K", "5K", "8K"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Résolution", cbResolutionEcran));

        slHzScore = makeSlider(1, 10, 5, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Score Hz", slHzScore, "/10"));

        cbDalleEcran = makeComboBox("Dalle", Arrays.asList("TN", "VA", "IPS", "OLED", "Mini-LED"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Dalle", cbDalleEcran));

        cbMarqueEcran = makeComboBox("Marque", service.getMarqueList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueEcran));
    }

    private void buildFormChaise() {
        slConfortChaise = makeSlider(1, 10, 7, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Confort", slConfortChaise, "/10"));

        slMateriauChaise = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Qualité matériau", slMateriauChaise, "/10"));

        chkRgbChaise = makeCheckBox("RGB / Éclairage");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRowSingle(chkRgbChaise));

        slInclinaisonChaise = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Inclinaison max", slInclinaisonChaise, "/10"));

        cbMarqueChaise = makeComboBox("Marque", Arrays.asList("Secretlab", "DXRacer", "Herman Miller", "AndaSeat", "IKEA", "NoName"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueChaise));
    }

    private void buildFormMaillot() {
        slQualiteMaillot = makeSlider(1, 10, 7, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Qualité tissu", slQualiteMaillot, "/10"));

        cbTailleMaillot = makeComboBox("Taille", Arrays.asList("XS", "S", "M", "L", "XL", "XXL"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Taille", cbTailleMaillot));

        cbEditionMaillot = makeComboBox("Édition", Arrays.asList("Standard", "Collector", "Limited", "Signed"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Édition", cbEditionMaillot));

        cbMarqueMaillot = makeComboBox("Marque / Équipe", service.getMarqueList());
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque / Équipe", cbMarqueMaillot));
    }

    private void buildFormTapis() {
        slTailleTapis = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Taille", slTailleTapis, "/10"));

        slEpaisseurTapis = makeSlider(1, 10, 5, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Épaisseur", slEpaisseurTapis, "/10"));

        chkRgbTapis = makeCheckBox("RGB");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRowSingle(chkRgbTapis));

        slMateriauTapis = makeSlider(1, 10, 6, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Qualité matériau", slMateriauTapis, "/10"));
    }

    private void buildFormManette() {
        slConfortManette = makeSlider(1, 10, 7, 0.5, "");
        dynamicFieldsContainer.getChildren().add(makeSliderRow("Confort", slConfortManette, "/10"));

        chkSansFilManette = makeCheckBox("Sans fil");
        chkVibrationManette = makeCheckBox("Vibration / Retour haptique");
        dynamicFieldsContainer.getChildren().add(makeCheckBoxRow(chkSansFilManette, chkVibrationManette));

        slCompatibiliteManette = makeSlider(1, 5, 3, 1, "");
        dynamicFieldsContainer.getChildren().add(
                makeSliderRowWithMap("Compatibilité", slCompatibiliteManette,
                        Map.of(1, "PC seul", 2, "PC+Xbox", 3, "Multi", 4, "Multi+Mobile", 5, "Universal")));

        cbMarqueManette = makeComboBox("Marque", Arrays.asList("Xbox", "PlayStation", "Razer", "Logitech", "8BitDo", "NoName"));
        dynamicFieldsContainer.getChildren().add(makeComboRow("Marque", cbMarqueManette));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Widget factory methods
    // ══════════════════════════════════════════════════════════════════════════

    private ComboBox<String> makeComboBox(String prompt, List<String> items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setPromptText("— " + prompt + " —");
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(COMBO_STYLE);
        if (!items.isEmpty()) cb.setValue(items.get(0));
        return cb;
    }

    private Slider makeSlider(double min, double max, double value, double tick, String unit) {
        Slider s = new Slider(min, max, value);
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setMajorTickUnit(tick * 4);
        s.setMinorTickCount(3);
        s.setBlockIncrement(tick);
        s.setStyle(SLIDER_STYLE);
        return s;
    }

    private CheckBox makeCheckBox(String label) {
        CheckBox cb = new CheckBox(label);
        cb.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 13px;");
        return cb;
    }

    /** Slider row with live value label */
    private VBox makeSliderRow(String label, Slider slider, String unit) {
        VBox box = new VBox(6);
        HBox header = new HBox(8);
        Label lbl = new Label(label);
        lbl.setStyle(LABEL_SECONDARY);
        Label valLbl = new Label(String.format("%.1f %s", slider.getValue(), unit));
        valLbl.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-font-weight: bold;");
        slider.valueProperty().addListener((obs, o, n) ->
                valLbl.setText(String.format("%.1f %s", n.doubleValue(), unit)));
        header.getChildren().addAll(lbl, valLbl);
        box.getChildren().addAll(header, slider);
        return box;
    }

    /** Slider row with discrete label map */
    private VBox makeSliderRowWithMap(String label, Slider slider, Map<Integer, String> labelMap) {
        VBox box = new VBox(6);
        HBox header = new HBox(8);
        Label lbl = new Label(label);
        lbl.setStyle(LABEL_SECONDARY);
        Label valLbl = new Label(labelMap.getOrDefault((int) slider.getValue(), ""));
        valLbl.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-font-weight: bold;");
        slider.valueProperty().addListener((obs, o, n) ->
                valLbl.setText(labelMap.getOrDefault((int) Math.round(n.doubleValue()), "")));
        header.getChildren().addAll(lbl, valLbl);
        box.getChildren().addAll(header, slider);
        return box;
    }

    private VBox makeComboRow(String label, ComboBox<String> cb) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.setStyle(LABEL_SECONDARY);
        box.getChildren().addAll(lbl, cb);
        return box;
    }

    private HBox makeCheckBoxRow(CheckBox cb1, CheckBox cb2) {
        HBox row = new HBox(24);
        row.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
        row.getChildren().addAll(cb1, cb2);
        return row;
    }

    private HBox makeCheckBoxRowSingle(CheckBox cb) {
        HBox row = new HBox(24);
        row.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");
        row.getChildren().add(cb);
        return row;
    }
}