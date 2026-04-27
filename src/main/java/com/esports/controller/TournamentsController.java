package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.model.Tournament;
import com.esports.service.TournamentService;
import com.esports.utils.AlertUtils;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TournamentsController implements Initializable {

    @FXML private Label lblTournamentCount;
    @FXML private TextField fieldSearch;
    @FXML private ComboBox<String> comboStatusFilter;
    @FXML private ComboBox<String> comboSortOrder;
    @FXML private FlowPane cardsContainer;

    private final ITournamentService tournamentService = new TournamentService();
    private List<Tournament> allTournaments;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        loadTournaments();
    }

    private void setupFilters() {
        comboStatusFilter.setItems(FXCollections.observableArrayList("Tous", "DRAFT", "OUVERT", "EN_COURS", "TERMINE"));
        comboStatusFilter.setValue("Tous");
        comboSortOrder.setItems(FXCollections.observableArrayList("Date (Décroissant)", "Date (Croissant)"));
        comboSortOrder.setValue("Date (Décroissant)");
        fieldSearch.textProperty().addListener((o, ov, nv) -> applyFilter());
        comboStatusFilter.valueProperty().addListener((o, ov, nv) -> applyFilter());
        comboSortOrder.valueProperty().addListener((o, ov, nv) -> applyFilter());
    }

    private void loadTournaments() {
        allTournaments = tournamentService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String search = fieldSearch.getText().toLowerCase().trim();
        String status = comboStatusFilter.getValue();
        List<Tournament> filtered = allTournaments.stream()
                .filter(t -> (search.isEmpty() || t.getNom().toLowerCase().contains(search) || t.getJeu().toLowerCase().contains(search)))
                .filter(t -> ("Tous".equals(status) || t.getStatut().equals(status)))
                .collect(Collectors.toList());
        if ("Date (Croissant)".equals(comboSortOrder.getValue()))
            filtered.sort(Comparator.comparing(Tournament::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())));
        else
            filtered.sort(Comparator.comparing(Tournament::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        renderCards(filtered);
        lblTournamentCount.setText(filtered.size() + " tournoi(s) affiché(s)");
    }

    private void renderCards(List<Tournament> list) {
        cardsContainer.getChildren().clear();
        for (Tournament t : list) cardsContainer.getChildren().add(createAdminCard(t));
    }

    private VBox createAdminCard(Tournament t) {
        VBox card = new VBox(10);
        card.setPrefSize(260, 320);
        card.setPadding(new Insets(0, 0, 15, 0));
        card.setStyle("-fx-background-color: #111122; -fx-border-color: #1a1a2e; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        ImageView iv = new ImageView();
        iv.setFitWidth(256); iv.setFitHeight(120); iv.setPreserveRatio(false);
        Rectangle clip = new Rectangle(256, 120); clip.setArcWidth(24); clip.setArcHeight(24); iv.setClip(clip);

        String path = t.getImage();
        boolean loaded = false;
        if (path != null && !path.trim().isEmpty()) {
            try {
                if (path.startsWith("http")) { iv.setImage(new Image(path, true)); loaded = true; }
                else {
                    File f = new File(path);
                    if (f.exists()) { iv.setImage(new Image(f.toURI().toString())); loaded = true; }
                    else {
                        URL res = getClass().getResource("/com/esports/images/" + path);
                        if (res != null) { iv.setImage(new Image(res.toString())); loaded = true; }
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!loaded) iv.setStyle("-fx-background-color: #1a1a2e;");

        VBox info = new VBox(8); info.setPadding(new Insets(10, 15, 10, 15));
        Label badge = new Label(t.getStatut());
        String color = switch (t.getStatut()) {
            case "OUVERT" -> "#00b8ff"; case "EN_COURS" -> "#00ff9d";
            case "TERMINE" -> "#555566"; case "DRAFT" -> "#ff6b35"; default -> "#888899";
        };
        badge.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 3 8; -fx-background-radius: 10;");
        Label name = new Label(t.getNom()); name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-wrap-text: true;");
        Label game = new Label("🎮 " + t.getJeu()); game.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px;");
        Label prize = new Label("🏆 " + String.format("%.0f €", t.getPrize())); prize.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px;");
        info.getChildren().addAll(badge, name, game, prize);

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER); actions.setPadding(new Insets(0, 15, 0, 15));
        Button btnEdit = new Button("Éditer");
        Button btnDelete = new Button("Supprimer");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #00b8ff; -fx-border-color: #00b8ff; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757; -fx-border-color: #ff4757; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
        btnEdit.setOnAction(e -> onEditTournament(t));
        btnDelete.setOnAction(e -> onDeleteTournament(t));
        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(iv, info, spacer, actions);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1a1a35; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #111122; -fx-border-color: #1a1a2e; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;"));
        return card;
    }

    @FXML
    private void onOpenCertificateDialog() {
        Stage stage = new Stage(); stage.initModality(Modality.APPLICATION_MODAL); stage.setTitle("Générer Certificat");
        VBox root = new VBox(15); root.setPadding(new Insets(30)); root.setStyle("-fx-background-color: #0f0d22;");
        Label title = new Label("GÉNÉRATEUR DE CERTIFICAT"); title.setStyle("-fx-text-fill: #ec4899; -fx-font-weight: bold; -fx-font-size: 18px;");
        TextField fNom = new TextField(); fNom.setPromptText("Nom"); fNom.setStyle("-fx-background-color: #1a1a35; -fx-text-fill: white;");
        TextField fPrenom = new TextField(); fPrenom.setPromptText("Prénom"); fPrenom.setStyle("-fx-background-color: #1a1a35; -fx-text-fill: white;");
        ComboBox<Tournament> cbTourneys = new ComboBox<>();
        cbTourneys.setItems(FXCollections.observableArrayList(tournamentService.findAll()));
        cbTourneys.setPromptText("Choisir un tournoi"); cbTourneys.setPrefWidth(300);
        Button btnGen = new Button("GÉNÉRER PDF");
        btnGen.setStyle("-fx-background-color: #ec4899; -fx-text-fill: white; -fx-font-weight: bold;"); btnGen.setMaxWidth(Double.MAX_VALUE);
        btnGen.setOnAction(e -> {
            if (fNom.getText().isEmpty() || fPrenom.getText().isEmpty() || cbTourneys.getValue() == null) return;
            generatePDF(fNom.getText(), fPrenom.getText(), cbTourneys.getValue()); stage.close();
        });
        root.getChildren().addAll(title, fNom, fPrenom, cbTourneys, btnGen);
        stage.setScene(new Scene(root, 400, 450)); stage.show();
    }

    private void generatePDF(String nom, String prenom, Tournament t) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("Certificat_" + nom + "_" + prenom + ".pdf");
        File file = chooser.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf, PageSize.A4.rotate());
                doc.setMargins(20, 20, 20, 20);
                DeviceRgb primaryColor = new DeviceRgb(236, 72, 153);
                DeviceRgb darkBg = new DeviceRgb(10, 8, 24);
                DeviceRgb gold = new DeviceRgb(251, 191, 36);
                Table borderTable = new Table(1).useAllAvailableWidth().setHeight(PageSize.A4.rotate().getHeight() - 40);
                Cell borderCell = new Cell().setBorder(new SolidBorder(primaryColor, 4)).setPadding(30).setBackgroundColor(darkBg);
                Table innerTable = new Table(1).useAllAvailableWidth().setHeight(PageSize.A4.rotate().getHeight() - 110).setBorder(new SolidBorder(gold, 1));
                Cell innerCell = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPadding(20);
                innerCell.add(new Paragraph("NEXUS ESPORTS PLATFORM").setFontSize(14).setBold().setFontColor(gold).setTextAlignment(TextAlignment.LEFT));
                innerCell.add(new Paragraph("\n\nCERTIFICAT DE PARTICIPATION").setFontSize(48).setBold().setFontColor(primaryColor).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
                innerCell.add(new Paragraph("DÉCERNÉ À").setFontSize(18).setFontColor(new DeviceRgb(168, 85, 247)).setTextAlignment(TextAlignment.CENTER).setMarginTop(30));
                innerCell.add(new Paragraph(prenom.toUpperCase() + " " + nom.toUpperCase()).setFontSize(36).setBold().setFontColor(com.itextpdf.kernel.colors.DeviceGray.WHITE).setTextAlignment(TextAlignment.CENTER));
                innerCell.add(new Paragraph("Pour avoir participé avec succès au tournoi").setFontSize(16).setFontColor(com.itextpdf.kernel.colors.DeviceGray.WHITE).setTextAlignment(TextAlignment.CENTER).setMarginTop(30));
                innerCell.add(new Paragraph(t.getJeu().toUpperCase() + " : " + t.getNom()).setFontSize(24).setItalic().setFontColor(gold).setTextAlignment(TextAlignment.CENTER));
                Table footerTable = new Table(2).useAllAvailableWidth().setMarginTop(50);
                footerTable.addCell(new Cell().add(new Paragraph("Date: " + java.time.LocalDate.now()).setFontSize(10).setFontColor(com.itextpdf.kernel.colors.DeviceGray.WHITE)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
                footerTable.addCell(new Cell().add(new Paragraph("Signature Officielle\nNexus Admin").setFontSize(10).setItalic().setFontColor(com.itextpdf.kernel.colors.DeviceGray.WHITE).setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
                innerCell.add(footerTable);
                innerTable.addCell(innerCell);
                borderCell.add(innerTable);
                borderTable.addCell(borderCell);
                doc.add(borderTable);
                doc.close();
                AlertUtils.showNotification((Stage) cardsContainer.getScene().getWindow(), "Certificat généré avec succès !", true);
            } catch (Exception ex) {
                ex.printStackTrace();
                AlertUtils.showNotification((Stage) cardsContainer.getScene().getWindow(), "Erreur lors de la génération PDF", false);
            }
        }
    }

    @FXML private void onShowStats() {
        Stage s = new Stage(); PieChart pc = new PieChart(); pc.setTitle("Statuts");
        tournamentService.findAll().stream().collect(Collectors.groupingBy(Tournament::getStatut, Collectors.counting()))
                .forEach((k, v) -> pc.getData().add(new PieChart.Data(k + " (" + v + ")", v)));
        s.setScene(new Scene(new VBox(pc), 600, 500)); s.show();
    }

    @FXML private void onAddTournament() { openTournamentForm(null); }
    private void onEditTournament(Tournament t) { openTournamentForm(t); }
    private void openTournamentForm(Tournament t) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/com/esports/fxml/TournamentForm.fxml"));
            Parent root = l.load();
            ((TournamentFormController) l.getController()).setTournament(t);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL); s.setScene(new Scene(root)); s.showAndWait();
            if (((TournamentFormController) l.getController()).isSaved()) {
                loadTournaments();
                AlertUtils.showNotification((Stage) cardsContainer.getScene().getWindow(), "Tournoi enregistré !", true);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void onDeleteTournament(Tournament t) {
        if (new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce tournoi ?").showAndWait().orElse(null) == ButtonType.OK) {
            if (tournamentService.delete(t.getId())) {
                loadTournaments();
                AlertUtils.showNotification((Stage) cardsContainer.getScene().getWindow(), "Tournoi supprimé !", true);
            } else {
                AlertUtils.showNotification((Stage) cardsContainer.getScene().getWindow(), "Erreur de suppression", false);
            }
        }
    }
}
