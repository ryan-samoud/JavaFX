package com.esports.controller;

import com.esports.interfaces.IEvenementService;
import com.esports.interfaces.ISponsorService;
import com.esports.model.Evenement;
import com.esports.model.Sponsor;
import com.esports.service.EvenementService;
import com.esports.service.SponsorService;
import com.esports.utils.NexusDialog;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CONTROLLER — EvenementsController.java
 * CRUD complet pour les événements + sponsors.
 * Fixes : description label color, FileChooser for image, confirmation dialogs.
 */
public class EvenementsController implements Initializable {

    // ── Images stockées dans src/main/resources/images/events/ ──
    private static final String IMAGES_FOLDER = "src/main/resources/images/events/";

    @FXML private Label            lblEventCount;
    @FXML private Label            lblFlagCount;
    @FXML private TextField        fieldSearch;
    @FXML private ComboBox<String> comboSort;

    @FXML private TableView<Evenement>            tableEvents;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String>  colNom;
    @FXML private TableColumn<Evenement, String>  colDate;
    @FXML private TableColumn<Evenement, String>  colLieu;
    @FXML private TableColumn<Evenement, Integer> colParticipants;
    @FXML private TableColumn<Evenement, String>  colStatut;
    @FXML private TableColumn<Evenement, Void>    colActions;

    private final IEvenementService evenementService = new EvenementService();
    private final ISponsorService   sponsorService   = new SponsorService();
    private final com.esports.service.CommentaireService commentaireService = new com.esports.service.CommentaireService();

    private ObservableList<Evenement> masterList  = FXCollections.observableArrayList();
    private FilteredList<Evenement>   filteredList;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try { new File(IMAGES_FOLDER).mkdirs(); } catch (Exception ignored) {}
        setupColumns();
        setupFilters();
        loadEvents();
        loadFlagCount();
    }

    // ══════════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════════

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate() != null
                        ? data.getValue().getDate().format(FMT) : "—"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colParticipants.setCellValueFactory(new PropertyValueFactory<>("nbrParticipant"));

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isPast() ? "PASSÉ" : "À VENIR"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = "PASSÉ".equals(item) ? "#555566" : "#00ff9d";
                setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit     = new Button("Éditer");
            private final Button btnDelete   = new Button("Supprimer");
            private final Button btnSponsors = new Button("Sponsors");
            private final HBox   box         = new HBox(6, btnEdit, btnDelete, btnSponsors);

            {
                btnEdit.setStyle(actionBtnStyle("#00b8ff"));
                btnDelete.setStyle(actionBtnStyle("#ff4757"));
                btnSponsors.setStyle(actionBtnStyle("#a855f7"));

                btnEdit.setOnAction(e    -> onEditEvent(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e  -> onDeleteEvent(getTableView().getItems().get(getIndex())));
                btnSponsors.setOnAction(e -> onManageSponsors(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private String actionBtnStyle(String color) {
        return "-fx-background-color: transparent; -fx-text-fill: " + color + ";" +
                "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                "-fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 4;" +
                "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;";
    }

    private void setupFilters() {
        comboSort.setItems(FXCollections.observableArrayList(
                "Date (récent)", "Date (ancien)", "Nom A→Z", "Nom Z→A",
                "Participants ↑", "Participants ↓"));
        comboSort.setValue("Date (récent)");

        filteredList = new FilteredList<>(masterList, p -> true);
        tableEvents.setItems(filteredList);

        fieldSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        comboSort.valueProperty().addListener((obs, o, n) -> applySort());
    }

    // ══════════════════════════════════════════════════
    // DONNÉES
    // ══════════════════════════════════════════════════

    private void loadEvents() {
        List<Evenement> list = evenementService.findAll();
        masterList.setAll(list);
        lblEventCount.setText(list.size() + " événement(s)");
    }

    private void applyFilter() {
        String search = fieldSearch.getText().toLowerCase().trim();
        filteredList.setPredicate(e -> search.isEmpty()
                || e.getNom().toLowerCase().contains(search)
                || e.getLieu().toLowerCase().contains(search));
        lblEventCount.setText(filteredList.size() + " événement(s) affiché(s)");
    }

    private void applySort() {
        String sort = comboSort.getValue();
        if (sort == null) return;
        List<Evenement> sorted = new java.util.ArrayList<>(masterList);
        switch (sort) {
            case "Date (récent)"  -> sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            case "Date (ancien)"  -> sorted.sort((a, b) -> a.getDate().compareTo(b.getDate()));
            case "Nom A→Z"        -> sorted.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Nom Z→A"        -> sorted.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            case "Participants ↑" -> sorted.sort((a, b) -> Integer.compare(a.getNbrParticipant(), b.getNbrParticipant()));
            case "Participants ↓" -> sorted.sort((a, b) -> Integer.compare(b.getNbrParticipant(), a.getNbrParticipant()));
        }
        masterList.setAll(sorted);
    }

    // ══════════════════════════════════════════════════
    // CRUD ACTIONS
    // ══════════════════════════════════════════════════

    @FXML
    private void onAddEvent() { showEventForm(null); }

    private void onEditEvent(Evenement e) { showEventForm(e); }

    private void onDeleteEvent(Evenement e) {
        boolean confirmed = NexusDialog.showConfirm(
                "Supprimer l'événement",
                "Supprimer « " + e.getNom() + " » ?",
                "Cette action est irréversible. Les sponsors associés seront également supprimés."
        );
        if (confirmed) {
            if (evenementService.delete(e.getId())) {
                masterList.remove(e);
                lblEventCount.setText(masterList.size() + " événement(s)");
                NexusDialog.showInfo("Succès", "L'événement a été supprimé.");
            }
        }
    }

    // ══════════════════════════════════════════════════
    // FORMULAIRE ÉVÉNEMENT
    // ══════════════════════════════════════════════════

    private void showEventForm(Evenement existing) {
        boolean isEdit = existing != null;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(isEdit ? "Modifier l'événement" : "Nouvel événement");
        stage.setResizable(false);

        // ── Champs ──
        TextField   fieldNom   = styledField("Nom de l'événement");
        TextArea    fieldDesc  = new TextArea();
        fieldDesc.setPromptText("Description de l'événement...");
        fieldDesc.setPrefRowCount(3);
        fieldDesc.setStyle(textAreaStyle());

        DatePicker  datePicker = new DatePicker();
        datePicker.setStyle(fieldStyle());
        datePicker.setPromptText("Choisir une date");

        TextField   fieldLieu  = styledField("Lieu (ville, adresse...)");

        // Image — affichage du nom du fichier choisi
        final String[] selectedImagePath = {null};
        HBox imageRow = new HBox(10);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        Label lblImageName = new Label("Aucune image choisie");
        lblImageName.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        Button btnPickImage = new Button("📁 Choisir une image");
        btnPickImage.setStyle("-fx-background-color: rgba(168,85,247,0.15); -fx-text-fill: #c084fc;" +
                "-fx-border-color: rgba(168,85,247,0.4); -fx-border-width: 1; -fx-border-radius: 6;" +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-padding: 8 14 8 14; -fx-cursor: hand;" +
                "-fx-font-family: 'Courier New';");
        btnPickImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                selectedImagePath[0] = file.getAbsolutePath();
                lblImageName.setText(file.getName());
                lblImageName.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
            }
        });
        imageRow.getChildren().addAll(btnPickImage, lblImageName);

        Label lblError = errorLabel();

        // Pré-remplissage si édition
        if (isEdit) {
            fieldNom.setText(existing.getNom());
            fieldDesc.setText(existing.getDescription() != null ? existing.getDescription() : "");
            datePicker.setValue(existing.getDate());
            fieldLieu.setText(existing.getLieu());
            if (existing.getImage() != null && !existing.getImage().isEmpty()) {
                lblImageName.setText(existing.getImage());
                lblImageName.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
            }
        }

        // ── Layout ──
        Label title = formTitle(isEdit ? "MODIFIER ÉVÉNEMENT" : "NOUVEL ÉVÉNEMENT");

        VBox form = new VBox(8,
                title, gradientSep(),
                formLabel("Nom *"),          fieldNom,
                formLabel("Description"),    fieldDesc,
                formLabel("Date *"),         datePicker,
                formLabel("Lieu *"),         fieldLieu,
                formLabel("Image"),          imageRow,
                lblError
        );
        form.setPadding(new Insets(28, 30, 10, 30));

        Button btnSave   = primaryBtn(isEdit ? "Enregistrer" : "Créer");
        Button btnCancel = cancelBtn("Annuler");
        HBox   btnRow    = new HBox(12, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 30, 28, 30));

        VBox root = new VBox(0, form, btnRow);
        root.setStyle(dialogStyle());

        // ── Validation & Sauvegarde ──
        btnSave.setOnAction(e -> {
            String    nom     = fieldNom.getText().trim();
            String    desc    = fieldDesc.getText().trim();
            LocalDate date    = datePicker.getValue();
            String    lieu    = fieldLieu.getText().trim();

            if (nom.isEmpty())     { lblError.setText("Le nom est obligatoire.");                          return; }
            if (nom.length() < 3)  { lblError.setText("Le nom doit contenir au moins 3 caractères.");     return; }
            int excludeId = isEdit ? existing.getId() : 0;
            if (evenementService.existsByNom(nom, excludeId)) {
                lblError.setText("Un événement avec ce nom existe déjà. Veuillez choisir un nom unique.");
                return;
            }
            if (date == null)      { lblError.setText("La date est obligatoire.");                          return; }
            if (lieu.isEmpty())    { lblError.setText("Le lieu est obligatoire.");                          return; }

            int nbr = 0;

            // Copier l'image dans le dossier events si une nouvelle image a été choisie
            String imageName = isEdit ? existing.getImage() : null;
            if (selectedImagePath[0] != null) {
                try {
                    File src  = new File(selectedImagePath[0]);
                    File dest = new File(IMAGES_FOLDER + src.getName());
                    dest.getParentFile().mkdirs();
                    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    imageName = src.getName();
                } catch (IOException ex) {
                    lblError.setText("Erreur lors de la copie de l'image : " + ex.getMessage());
                    return;
                }
            }

            // Confirmation avant sauvegarde
            boolean confirmed = NexusDialog.showConfirm(
                    isEdit ? "Modifier l'événement" : "Créer l'événement",
                    isEdit ? "Modifier « " + nom + " » ?" : "Créer l'événement « " + nom + " » ?",
                    "Veuillez confirmer cette action."
            );
            if (!confirmed) return;

            Evenement ev;

            if (isEdit) {
                ev = existing;

                ev.setNom(nom);
                ev.setDescription(desc);
                ev.setDate(date);
                ev.setLieu(lieu);
                ev.setImage(imageName);

            } else {
                ev = new Evenement(nom, desc, date, lieu, 0, imageName);
            }

            boolean ok = isEdit ? evenementService.update(ev) : evenementService.save(ev);
            if (ok) {
                stage.close();
                loadEvents();
            } else {
                lblError.setText("Erreur lors de la sauvegarde. Vérifiez la console.");
            }
        });

        btnCancel.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 480, 600);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════
    // GESTION SPONSORS
    // ══════════════════════════════════════════════════

    private void onManageSponsors(Evenement ev) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Sponsors — " + ev.getNom());
        stage.setResizable(false);

        TableView<Sponsor> table = new TableView<>();
        table.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        table.setPrefHeight(220);

        TableColumn<Sponsor, Integer> cId    = col("ID",      40);
        TableColumn<Sponsor, String>  cNom   = col("NOM",     120);
        TableColumn<Sponsor, String>  cType  = col("TYPE",    90);
        TableColumn<Sponsor, String>  cEmail = col("EMAIL",   140);
        TableColumn<Sponsor, String>  cTel   = col("TÉL",    90);
        TableColumn<Sponsor, String>  cPrix  = col("BUDGET", 70);
        TableColumn<Sponsor, Void>    cAct   = new TableColumn<>("ACTIONS");
        cAct.setPrefWidth(120);

        cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        cType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeLabel()));
        cEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        cTel.setCellValueFactory(new PropertyValueFactory<>("tel"));
        cPrix.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f €", d.getValue().getPrix())));

        ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList(
                sponsorService.findByEvenement(ev.getId()));
        table.setItems(sponsorList);

        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button btnE = new Button("Éditer");
            private final Button btnD = new Button("Suppr.");
            private final HBox   box  = new HBox(6, btnE, btnD);
            {
                btnE.setStyle(actionBtnStyle("#00b8ff").replace("11px", "10px"));
                btnD.setStyle(actionBtnStyle("#ff4757").replace("11px", "10px"));
                btnE.setOnAction(e -> showSponsorForm(getTableView().getItems().get(getIndex()), ev, sponsorList));
                btnD.setOnAction(e -> {
                    Sponsor s = getTableView().getItems().get(getIndex());
                    boolean ok = NexusDialog.showConfirm("Supprimer le sponsor",
                            "Supprimer « " + s.getNom() + " » ?", "Cette action est irréversible.");
                    if (ok && sponsorService.delete(s.getId())) {
                        sponsorList.remove(s);
                        NexusDialog.showInfo("Succès", "Sponsor supprimé.");
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(cId, cNom, cType, cEmail, cTel, cPrix, cAct);

        Label title = formTitle("SPONSORS — " + ev.getNom().toUpperCase());

        Button btnAdd   = primaryBtn("＋ Ajouter sponsor");
        Button btnClose = cancelBtn("Fermer");
        btnAdd.setOnAction(e -> showSponsorForm(null, ev, sponsorList));
        btnClose.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(12, btnClose, btnAdd);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(14, 0, 0, 0));

        VBox root = new VBox(14, title, gradientSep(), table, btnRow);
        root.setPadding(new Insets(28, 30, 28, 30));
        root.setStyle(dialogStyle());

        Scene scene = new Scene(root, 730, 400);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private <T> TableColumn<Sponsor, T> col(String text, int width) {
        TableColumn<Sponsor, T> c = new TableColumn<>(text);
        c.setPrefWidth(width);
        return c;
    }

    private void showSponsorForm(Sponsor existing, Evenement ev, ObservableList<Sponsor> list) {
        boolean isEdit = existing != null;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        TextField        fieldNom   = styledField("Nom du sponsor");
        ComboBox<String> comboType  = new ComboBox<>(FXCollections.observableArrayList("entreprise", "humain"));
        comboType.setStyle(fieldStyle()); comboType.setValue("entreprise");
        TextField        fieldEmail = styledField("Email");
        TextField        fieldTel   = styledField("Téléphone (8 chiffres)");
        TextField        fieldPrix  = styledField("Budget (€)");
        Label            lblError   = errorLabel();

        if (isEdit) {
            fieldNom.setText(existing.getNom());
            comboType.setValue(existing.getType());
            fieldEmail.setText(existing.getEmail()  != null ? existing.getEmail()  : "");
            fieldTel.setText(existing.getTel()    != null ? existing.getTel()    : "");
            fieldPrix.setText(String.valueOf((int) existing.getPrix()));
        }

        Label title = formTitle(isEdit ? "MODIFIER SPONSOR" : "NOUVEAU SPONSOR");

        VBox form = new VBox(8,
                title, gradientSep(),
                formLabel("Nom *"),          fieldNom,
                formLabel("Type *"),         comboType,
                formLabel("Email"),          fieldEmail,
                formLabel("Téléphone"),      fieldTel,
                formLabel("Budget (€) *"),   fieldPrix,
                lblError
        );
        form.setPadding(new Insets(28, 30, 10, 30));

        Button btnSave   = primaryBtn(isEdit ? "Enregistrer" : "Créer");
        Button btnCancel = cancelBtn("Annuler");
        HBox   btnRow    = new HBox(12, btnCancel, btnSave);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 30, 28, 30));

        VBox root = new VBox(0, form, btnRow);
        root.setStyle(dialogStyle());

        btnSave.setOnAction(e -> {
            String nom     = fieldNom.getText().trim();
            String type    = comboType.getValue();
            String email   = fieldEmail.getText().trim();
            String tel     = fieldTel.getText().trim();
            String prixStr = fieldPrix.getText().trim();

            if (nom.isEmpty())    { lblError.setText("Le nom est obligatoire.");               return; }
            if (nom.length() < 2) { lblError.setText("Le nom doit contenir au moins 2 caractères."); return; }

            if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                lblError.setText("Format d'email invalide."); return;
            }

            if (!tel.isEmpty() && !tel.matches("\\d{8}")) {
                lblError.setText("Le téléphone doit contenir exactement 8 chiffres."); return;
            }

            double prix = 0;
            if (!prixStr.isEmpty()) {
                try {
                    prix = Double.parseDouble(prixStr);
                    if (prix < 0) { lblError.setText("Le budget doit être positif."); return; }
                } catch (NumberFormatException ex) {
                    lblError.setText("Le budget doit être un nombre."); return;
                }
            }

            boolean confirmed = NexusDialog.showConfirm(
                    isEdit ? "Modifier le sponsor" : "Créer le sponsor",
                    isEdit ? "Modifier « " + nom + " » ?" : "Créer le sponsor « " + nom + " » ?",
                    "Veuillez confirmer cette action."
            );
            if (!confirmed) return;

            Sponsor s = isEdit ? existing
                    : new Sponsor(nom, type, email.isEmpty() ? null : email,
                    tel.isEmpty() ? null : tel, prix, ev.getId());
            if (isEdit) {
                s.setNom(nom); s.setType(type);
                s.setEmail(email.isEmpty() ? null : email);
                s.setTel(tel.isEmpty()     ? null : tel);
                s.setPrix(prix);
            }

            boolean ok = isEdit ? sponsorService.update(s) : sponsorService.save(s);
            if (ok) {
                stage.close();
                list.setAll(sponsorService.findByEvenement(ev.getId()));
            } else {
                lblError.setText("Erreur lors de la sauvegarde.");
            }
        });

        btnCancel.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 420, 550);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════
    // HELPERS UI
    // ══════════════════════════════════════════════════

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private String fieldStyle() {
        return "-fx-background-color: #1a1035; -fx-text-fill: #e2e8f0;" +
                "-fx-prompt-text-fill: #4b5563; -fx-border-color: rgba(139,92,246,0.3);" +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-padding: 10 14 10 14;";
    }

    private String textAreaStyle() {
        return "-fx-background-color: #1a1035; -fx-text-fill: #e2e8f0;" +
                "-fx-prompt-text-fill: #4b5563; -fx-border-color: rgba(139,92,246,0.3);" +
                "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-padding: 10 14 10 14;" +
                "-fx-control-inner-background: #1a1035;";
    }

    // FIX: description label — same style as all other labels
    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-font-family: 'Courier New'; -fx-letter-spacing: 1px;");
        return l;
    }

    private Label formTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;" +
                "-fx-font-family: 'Courier New'; -fx-letter-spacing: 2px;");
        return l;
    }

    private Label errorLabel() {
        Label l = new Label("");
        l.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        l.setWrapText(true);
        return l;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        String s = "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;" +
                "-fx-border-color: transparent;";
        b.setStyle(s);
        b.setOnMouseEntered(e -> b.setStyle(s.replace("#7c3aed","#6d28d9").replace("#ec4899","#db2777")));
        b.setOnMouseExited(e  -> b.setStyle(s));
        return b;
    }

    private Button cancelBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8px;" +
                "-fx-padding: 9 28 9 28; -fx-cursor: hand;" +
                "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 1.5px; -fx-border-radius: 8px;");
        return b;
    }

    private Node gradientSep() {
        Region sep = new Region();
        sep.setPrefHeight(1.5);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, rgba(236,72,153,0.5), transparent);");
        VBox.setMargin(sep, new Insets(6, 0, 6, 0));
        return sep;
    }

    private String dialogStyle() {
        return "-fx-background-color: #110f28;" +
                "-fx-border-color: rgba(139,92,246,0.45); -fx-border-width: 1.5px;" +
                "-fx-border-radius: 14px; -fx-background-radius: 14px;" +
                "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.5), 30, 0.3, 0, 6);";
    }

    // ══════════════════════════════════════════════════
    // FLAGGED COMMENTS NOTIFICATION
    // ══════════════════════════════════════════════════

    private void loadFlagCount() {
        if (lblFlagCount == null) return;
        int count = commentaireService.findFlagged().size();
        if (count > 0) {
            lblFlagCount.setText(String.valueOf(count));
            lblFlagCount.setVisible(true);
            lblFlagCount.setManaged(true);
        } else {
            lblFlagCount.setVisible(false);
            lblFlagCount.setManaged(false);
        }
    }

    @FXML
    private void onShowFlagged() {
        java.util.List<com.esports.model.Commentaire> flagged = commentaireService.findFlagged();

        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setTitle("Commentaires signalés");
        stage.setResizable(false);

        Label title = formTitle("🚩 COMMENTAIRES SIGNALÉS");

        javafx.scene.Node sep = gradientSep();

        javafx.scene.layout.VBox list = new javafx.scene.layout.VBox(10);
        list.setMaxHeight(420);

        if (flagged.isEmpty()) {
            Label empty = new Label("Aucun commentaire signalé.");
            empty.setStyle("-fx-text-fill: #4ade80; -fx-font-family: 'Courier New'; -fx-font-size: 13px;");
            list.getChildren().add(empty);
        } else {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (com.esports.model.Commentaire c : flagged) {
                javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(6);
                card.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));
                card.setStyle("-fx-background-color: rgba(239,68,68,0.08);" +
                        "-fx-border-color: rgba(239,68,68,0.3); -fx-border-width: 1;" +
                        "-fx-border-radius: 8px; -fx-background-radius: 8px;");

                javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
                header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label auteur = new Label("👤 " + (c.getAuteurNom() != null ? c.getAuteurNom() : "Utilisateur"));
                auteur.setStyle("-fx-text-fill: #c084fc; -fx-font-weight: bold; -fx-font-size: 12px; -fx-font-family: 'Courier New';");

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label dateLabel = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(fmt) : "");
                dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10px; -fx-font-family: 'Courier New';");

                // Approve button
                Button btnApprove = new Button("✓ Approuver");
                btnApprove.setStyle("-fx-background-color: rgba(74,222,128,0.15); -fx-text-fill: #4ade80;" +
                        "-fx-border-color: rgba(74,222,128,0.3); -fx-border-width: 1;" +
                        "-fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 10px; -fx-padding: 3 10 3 10; -fx-cursor: hand; -fx-font-family: 'Courier New';");
                btnApprove.setOnAction(e -> {
                    commentaireService.approveFlagged(c.getId());
                    list.getChildren().remove(card);
                    loadFlagCount();
                    if (list.getChildren().isEmpty()) {
                        Label empty = new Label("Aucun commentaire signalé.");
                        empty.setStyle("-fx-text-fill: #4ade80; -fx-font-family: 'Courier New'; -fx-font-size: 13px;");
                        list.getChildren().add(empty);
                    }
                });

                // Delete button
                Button btnDel = new Button("🗑 Supprimer");
                btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #f87171;" +
                        "-fx-border-color: rgba(239,68,68,0.3); -fx-border-width: 1;" +
                        "-fx-border-radius: 4; -fx-background-radius: 4;" +
                        "-fx-font-size: 10px; -fx-padding: 3 10 3 10; -fx-cursor: hand; -fx-font-family: 'Courier New';");
                btnDel.setOnAction(e -> {
                    boolean ok = NexusDialog.showConfirm("Supprimer", "Supprimer ce commentaire ?", "Cette action est irréversible.");
                    if (ok && commentaireService.delete(c.getId())) {
                        list.getChildren().remove(card);
                        loadFlagCount();
                        if (list.getChildren().isEmpty()) {
                            Label empty = new Label("Aucun commentaire signalé.");
                            empty.setStyle("-fx-text-fill: #4ade80; -fx-font-family: 'Courier New'; -fx-font-size: 13px;");
                            list.getChildren().add(empty);
                        }
                    }
                });

                header.getChildren().addAll(auteur, spacer, dateLabel, btnApprove, btnDel);

                Label contenu = new Label(c.getContenu());
                contenu.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
                contenu.setWrapText(true);

                card.getChildren().addAll(header, contenu);
                list.getChildren().add(card);
            }
        }

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background: transparent;");
        scroll.setMaxHeight(420);

        Button btnClose = cancelBtn("Fermer");
        btnClose.setOnAction(e -> stage.close());
        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(btnClose);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        btnRow.setPadding(new javafx.geometry.Insets(10, 30, 28, 30));

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(0);
        javafx.scene.layout.VBox inner = new javafx.scene.layout.VBox(10, title, sep, scroll);
        inner.setPadding(new javafx.geometry.Insets(28, 30, 10, 30));
        content.getChildren().addAll(inner, btnRow);
        content.setStyle(dialogStyle());

        javafx.scene.Scene scene = new javafx.scene.Scene(content, 560, 560);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }
}