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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CONTROLLER — EvenementsController.java
 * CRUD complet pour les événements + sponsors.
 */
public class EvenementsController implements Initializable {

    @FXML private Label              lblEventCount;
    @FXML private TextField          fieldSearch;
    @FXML private ComboBox<String>   comboSort;

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

    private ObservableList<Evenement> masterList   = FXCollections.observableArrayList();
    private FilteredList<Evenement>   filteredList;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        loadEvents();
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

        // Badge statut A VENIR / PASSÉ
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

        // Colonne actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit    = new Button("Éditer");
            private final Button btnDelete  = new Button("Supprimer");
            private final Button btnSponsors = new Button("Sponsors");
            private final HBox   box        = new HBox(6, btnEdit, btnDelete, btnSponsors);

            {
                btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #00b8ff;" +
                        "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                        "-fx-border-color: #00b8ff; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757;" +
                        "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                        "-fx-border-color: #ff4757; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btnSponsors.setStyle("-fx-background-color: transparent; -fx-text-fill: #a855f7;" +
                        "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                        "-fx-border-color: #a855f7; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");

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

    private void setupFilters() {
        comboSort.setItems(FXCollections.observableArrayList(
                "Date (récent)", "Date (ancien)", "Nom A→Z", "Nom Z→A", "Participants ↑", "Participants ↓"));
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
            case "Date (récent)"    -> sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            case "Date (ancien)"    -> sorted.sort((a, b) -> a.getDate().compareTo(b.getDate()));
            case "Nom A→Z"          -> sorted.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Nom Z→A"          -> sorted.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            case "Participants ↑"   -> sorted.sort((a, b) -> Integer.compare(a.getNbrParticipant(), b.getNbrParticipant()));
            case "Participants ↓"   -> sorted.sort((a, b) -> Integer.compare(b.getNbrParticipant(), a.getNbrParticipant()));
        }
        masterList.setAll(sorted);
    }

    // ══════════════════════════════════════════════════
    // CRUD ACTIONS
    // ══════════════════════════════════════════════════

    @FXML
    private void onAddEvent() {
        showEventForm(null);
    }

    private void onEditEvent(Evenement e) {
        showEventForm(e);
    }

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
    // FORMULAIRE ÉVÉNEMENT (Ajout / Édition)
    // ══════════════════════════════════════════════════

    private void showEventForm(Evenement existing) {
        boolean isEdit = existing != null;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(isEdit ? "Modifier l'événement" : "Nouvel événement");
        stage.setResizable(false);

        // ── Champs ──
        TextField    fieldNom    = styledField("Nom de l'événement");
        TextArea     fieldDesc   = new TextArea();
        fieldDesc.setPromptText("Description");
        fieldDesc.setPrefRowCount(3);
        fieldDesc.setStyle(textAreaStyle());
        DatePicker   datePicker  = new DatePicker();
        datePicker.setStyle(fieldStyle());
        datePicker.setPromptText("Date");
        TextField    fieldLieu   = styledField("Lieu (ville, adresse...)");
        TextField    fieldPart   = styledField("Nombre de participants");
        TextField    fieldImage  = styledField("Image (URL ou chemin)");
        Label        lblError    = errorLabel();

        if (isEdit) {
            fieldNom.setText(existing.getNom());
            fieldDesc.setText(existing.getDescription());
            datePicker.setValue(existing.getDate());
            fieldLieu.setText(existing.getLieu());
            fieldPart.setText(String.valueOf(existing.getNbrParticipant()));
            fieldImage.setText(existing.getImage() != null ? existing.getImage() : "");
        }

        // ── Layout ──
        Label title = formTitle(isEdit ? "MODIFIER ÉVÉNEMENT" : "NOUVEL ÉVÉNEMENT");

        VBox form = new VBox(12,
                title,
                gradientSep(),
                formLabel("Nom *"), fieldNom,
                formLabel("Description"), fieldDesc,
                formLabel("Date *"), datePicker,
                formLabel("Lieu *"), fieldLieu,
                formLabel("Participants"), fieldPart,
                formLabel("Image"), fieldImage,
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
            String nom    = fieldNom.getText().trim();
            String desc   = fieldDesc.getText().trim();
            LocalDate date = datePicker.getValue();
            String lieu   = fieldLieu.getText().trim();
            String partStr = fieldPart.getText().trim();
            String image  = fieldImage.getText().trim();

            // Validations
            if (nom.isEmpty()) { lblError.setText("Le nom est obligatoire."); return; }
            if (nom.length() < 3) { lblError.setText("Le nom doit contenir au moins 3 caractères."); return; }
            if (date == null) { lblError.setText("La date est obligatoire."); return; }
            if (lieu.isEmpty()) { lblError.setText("Le lieu est obligatoire."); return; }

            int nbr = 0;
            if (!partStr.isEmpty()) {
                try {
                    nbr = Integer.parseInt(partStr);
                    if (nbr < 0) { lblError.setText("Le nombre de participants doit être positif."); return; }
                } catch (NumberFormatException ex) {
                    lblError.setText("Le nombre de participants doit être un entier.");
                    return;
                }
            }

            Evenement ev = isEdit ? existing
                    : new Evenement(nom, desc, date, lieu, nbr, image.isEmpty() ? null : image);
            if (isEdit) {
                ev.setNom(nom); ev.setDescription(desc); ev.setDate(date);
                ev.setLieu(lieu); ev.setNbrParticipant(nbr);
                ev.setImage(image.isEmpty() ? null : image);
            }

            boolean ok = isEdit ? evenementService.update(ev) : evenementService.save(ev);
            if (ok) {
                stage.close();
                loadEvents();
                NexusDialog.showInfo("Succès", isEdit ? "Événement modifié." : "Événement créé.");
            } else {
                lblError.setText("Erreur lors de la sauvegarde.");
            }
        });

        btnCancel.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 480, 620);
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

        // Table sponsors
        TableView<Sponsor> table = new TableView<>();
        table.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        table.setPrefHeight(220);

        TableColumn<Sponsor, Integer> cId    = new TableColumn<>("ID");
        TableColumn<Sponsor, String>  cNom   = new TableColumn<>("NOM");
        TableColumn<Sponsor, String>  cType  = new TableColumn<>("TYPE");
        TableColumn<Sponsor, String>  cEmail = new TableColumn<>("EMAIL");
        TableColumn<Sponsor, String>  cTel   = new TableColumn<>("TÉL");
        TableColumn<Sponsor, String>  cPrix  = new TableColumn<>("BUDGET");
        TableColumn<Sponsor, Void>    cAct   = new TableColumn<>("ACTIONS");

        cId.setCellValueFactory(new PropertyValueFactory<>("id"));        cId.setPrefWidth(40);
        cNom.setCellValueFactory(new PropertyValueFactory<>("nom"));      cNom.setPrefWidth(120);
        cType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTypeLabel())); cType.setPrefWidth(90);
        cEmail.setCellValueFactory(new PropertyValueFactory<>("email"));  cEmail.setPrefWidth(140);
        cTel.setCellValueFactory(new PropertyValueFactory<>("tel"));      cTel.setPrefWidth(90);
        cPrix.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.0f €", d.getValue().getPrix())));        cPrix.setPrefWidth(70);

        ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList(
                sponsorService.findByEvenement(ev.getId()));
        table.setItems(sponsorList);

        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button btnE = new Button("Éditer");
            private final Button btnD = new Button("Suppr.");
            private final HBox   box  = new HBox(6, btnE, btnD);
            {
                btnE.setStyle("-fx-background-color: transparent; -fx-text-fill: #00b8ff;" +
                        "-fx-border-color: #00b8ff; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-font-size: 10px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
                btnD.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757;" +
                        "-fx-border-color: #ff4757; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-font-size: 10px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
                btnE.setOnAction(e -> showSponsorForm(getTableView().getItems().get(getIndex()), ev, sponsorList));
                btnD.setOnAction(e -> {
                    Sponsor s = getTableView().getItems().get(getIndex());
                    boolean ok = NexusDialog.showConfirm("Supprimer le sponsor",
                            "Supprimer « " + s.getNom() + " » ?", "Cette action est irréversible.");
                    if (ok && sponsorService.delete(s.getId())) {
                        sponsorList.remove(s);
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : box);
            }
        });
        cAct.setPrefWidth(120);

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

        Scene scene = new Scene(root, 720, 400);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void showSponsorForm(Sponsor existing, Evenement ev, ObservableList<Sponsor> list) {
        boolean isEdit = existing != null;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        TextField         fieldNom   = styledField("Nom du sponsor");
        ComboBox<String>  comboType  = new ComboBox<>(FXCollections.observableArrayList("entreprise", "humain"));
        comboType.setStyle(fieldStyle()); comboType.setValue("entreprise");
        TextField         fieldEmail = styledField("Email");
        TextField         fieldTel   = styledField("Téléphone (8 chiffres)");
        TextField         fieldPrix  = styledField("Budget (€)");
        Label             lblError   = errorLabel();

        if (isEdit) {
            fieldNom.setText(existing.getNom());
            comboType.setValue(existing.getType());
            fieldEmail.setText(existing.getEmail() != null ? existing.getEmail() : "");
            fieldTel.setText(existing.getTel() != null ? existing.getTel() : "");
            fieldPrix.setText(String.valueOf((int) existing.getPrix()));
        }

        Label title = formTitle(isEdit ? "MODIFIER SPONSOR" : "NOUVEAU SPONSOR");

        VBox form = new VBox(10,
                title, gradientSep(),
                formLabel("Nom *"), fieldNom,
                formLabel("Type *"), comboType,
                formLabel("Email"), fieldEmail,
                formLabel("Téléphone"), fieldTel,
                formLabel("Budget (€) *"), fieldPrix,
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
            String nom   = fieldNom.getText().trim();
            String type  = comboType.getValue();
            String email = fieldEmail.getText().trim();
            String tel   = fieldTel.getText().trim();
            String prixStr = fieldPrix.getText().trim();

            // Validations
            if (nom.isEmpty())   { lblError.setText("Le nom est obligatoire."); return; }
            if (nom.length() < 2){ lblError.setText("Le nom doit contenir au moins 2 caractères."); return; }

            if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                lblError.setText("Format d'email invalide."); return;
            }

            if (!tel.isEmpty()) {
                if (!tel.matches("\\d{8}")) {
                    lblError.setText("Le téléphone doit contenir exactement 8 chiffres."); return;
                }
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

            Sponsor s = isEdit ? existing
                    : new Sponsor(nom, type, email.isEmpty() ? null : email,
                            tel.isEmpty() ? null : tel, prix, ev.getId());
            if (isEdit) {
                s.setNom(nom); s.setType(type);
                s.setEmail(email.isEmpty() ? null : email);
                s.setTel(tel.isEmpty() ? null : tel);
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

        Scene scene = new Scene(root, 420, 480);
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
               "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-padding: 10 14 10 14;";
    }

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
        b.setOnMouseEntered(e -> b.setStyle(s.replace("#7c3aed", "#6d28d9").replace("#ec4899","#db2777")));
        b.setOnMouseExited(e  -> b.setStyle(s));
        return b;
    }

    private Button cancelBtn(String text) {
        Button b = new Button(text);
        String s = "-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                   "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8px;" +
                   "-fx-padding: 9 28 9 28; -fx-cursor: hand;" +
                   "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 1.5px; -fx-border-radius: 8px;";
        b.setStyle(s);
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
               "-fx-border-color: rgba(139,92,246,0.45);" +
               "-fx-border-width: 1.5px; -fx-border-radius: 14px; -fx-background-radius: 14px;" +
               "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.5), 30, 0.3, 0, 6);";
    }
}
