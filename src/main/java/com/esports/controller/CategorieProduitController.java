package com.esports.controller;

import com.esports.model.CategorieProduit;
import com.esports.service.CategorieProduitService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CategorieProduitController implements Initializable {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Label lblCount;

    // ── Filtre ───────────────────────────────────────────────────
    @FXML private TextField fieldSearch;

    // ── Table ────────────────────────────────────────────────────
    @FXML private TableView<CategorieProduit>            tableCategories;
    @FXML private TableColumn<CategorieProduit, Integer> colId;
    @FXML private TableColumn<CategorieProduit, String>  colNom;
    @FXML private TableColumn<CategorieProduit, String>  colDescription;
    @FXML private TableColumn<CategorieProduit, String>  colDateCreation;
    @FXML private TableColumn<CategorieProduit, String>  colStatut;
    @FXML private TableColumn<CategorieProduit, String>  colActions;

    // ── Formulaire ────────────────────────────────────────────────
    @FXML private VBox      formPanel;
    @FXML private Label     lblFormTitle;
    @FXML private TextField fieldNom;
    @FXML private TextArea  fieldDescription;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Label     lblFormError;

    // ── Service ───────────────────────────────────────────────────
    private final CategorieProduitService service = new CategorieProduitService();
    private ObservableList<CategorieProduit> categories = FXCollections.observableArrayList();
    private CategorieProduit selected = null;

    // ══════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSearch();
        setupForm();
        loadData();
        if (formPanel != null) formPanel.setVisible(false);
    }

    // ── Table ─────────────────────────────────────────────────────
    private void setupTable() {
        if (colId != null)
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        if (colNom != null)
            colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        if (colDescription != null)
            colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Colonne date_creation
        if (colDateCreation != null) {
            colDateCreation.setCellValueFactory(data -> {
                CategorieProduit c = data.getValue();
                String date = c.getDateCreation() != null
                        ? c.getDateCreation().format(DATE_FMT)
                        : "—";
                return new SimpleStringProperty(date);
            });
        }

        // Colonne statut — badge coloré Actif / Inactif
        if (colStatut != null) {
            colStatut.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    CategorieProduit c = getTableView().getItems().get(getIndex());
                    Label badge = new Label(c.isActive() ? "● Actif" : "○ Inactif");
                    badge.setStyle(c.isActive()
                        ? "-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;" +
                          "-fx-background-color: rgba(74,222,128,0.12);" +
                          "-fx-border-color: rgba(74,222,128,0.3); -fx-border-width: 1px;" +
                          "-fx-border-radius: 20px; -fx-background-radius: 20px;" +
                          "-fx-padding: 3 12 3 12;"
                        : "-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: bold;" +
                          "-fx-background-color: rgba(248,113,113,0.12);" +
                          "-fx-border-color: rgba(248,113,113,0.3); -fx-border-width: 1px;" +
                          "-fx-border-radius: 20px; -fx-background-radius: 20px;" +
                          "-fx-padding: 3 12 3 12;"
                    );
                    setGraphic(badge);
                }
            });
        }

        // Colonne Actions — Modifier + Toggle statut + Supprimer
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit   = buildBtn("= Modi...", "#f59e0b", "#422006");
                private final Button btnToggle = buildBtn("O/I", "#a855f7", "#2e1065");
                private final Button btnDelete = buildBtn("X Del", "#f87171", "#450a0a");
                private final HBox box = new HBox(6, btnEdit, btnToggle, btnDelete);

                {
                    btnEdit.setTooltip(new Tooltip("Modifier"));
                    btnToggle.setTooltip(new Tooltip("Activer / Désactiver"));
                    btnDelete.setTooltip(new Tooltip("Supprimer"));

                    btnEdit.setOnAction(e -> {
                        CategorieProduit c = getTableView().getItems().get(getIndex());
                        boolean ok = com.esports.utils.NexusDialog.showConfirm(
                            "Modifier la cat\u00e9gorie",
                            "Modifier \"" + c.getNom() + "\" ?",
                            "Vous allez ouvrir le formulaire de modification."
                        );
                        if (ok) onEdit(c);
                    });
                    btnToggle.setOnAction(e -> {
                        CategorieProduit c = getTableView().getItems().get(getIndex());
                        onToggleStatut(c);
                    });
                    btnDelete.setOnAction(e -> {
                        CategorieProduit c = getTableView().getItems().get(getIndex());
                        onDelete(c);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    CategorieProduit c = getTableView().getItems().get(getIndex());
                    btnToggle.setText(c.isActive() ? "OFF" : "ON");
                    setGraphic(box);
                }
            });
        }

        if (tableCategories != null) tableCategories.setItems(categories);
    }

    // ── Recherche ─────────────────────────────────────────────────
    private void setupSearch() {
        if (fieldSearch != null)
            fieldSearch.textProperty().addListener((o, a, b) -> {
                String q = b.trim();
                categories.setAll(q.isEmpty() ? service.findAll() : service.search(q));
                updateCount();
            });
    }

    // ── Formulaire statut ComboBox ────────────────────────────────
    private void setupForm() {
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("Actif", "Inactif"));
            comboStatut.setValue("Actif");
        }
    }

    private void loadData() {
        categories.setAll(service.findAll());
        updateCount();
    }

    private void updateCount() {
        if (lblCount != null) lblCount.setText(categories.size() + " catégorie(s)");
    }

    // ══════════════════════════════════════════════════════════════
    // ACTIONS CRUD
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onAdd() {
        selected = null;
        clearForm();
        if (lblFormTitle  != null) lblFormTitle.setText("➕  Nouvelle Catégorie");
        if (comboStatut   != null) comboStatut.setValue("Actif");
        if (formPanel     != null) formPanel.setVisible(true);
    }

    private void onEdit(CategorieProduit c) {
        selected = c;
        if (lblFormTitle    != null) lblFormTitle.setText("=  Modifier : " + c.getNom());
        if (fieldNom        != null) fieldNom.setText(c.getNom());
        if (fieldDescription != null) fieldDescription.setText(c.getDescription());
        if (comboStatut     != null) comboStatut.setValue(c.isActive() ? "Actif" : "Inactif");
        if (formPanel       != null) formPanel.setVisible(true);
    }

    // Toggle actif/inactif directement depuis le tableau
    private void onToggleStatut(CategorieProduit c) {
        c.setStatut(c.isActive() ? 0 : 1);
        if (service.update(c)) {
            tableCategories.refresh();
        } else {
            c.setStatut(c.isActive() ? 0 : 1); // rollback
        }
    }

    private void onDelete(CategorieProduit c) {
        boolean confirmed = com.esports.utils.NexusDialog.showConfirm(
            "Supprimer la catégorie",
            "Supprimer \"" + c.getNom() + "\" ?",
            "Les produits liés resteront sans catégorie."
        );
        if (confirmed) {
            if (service.delete(c.getId())) loadData();
        }
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        String nom    = fieldNom.getText().trim();
        String desc   = fieldDescription != null ? fieldDescription.getText().trim() : "";
        int    statut = (comboStatut != null && "Actif".equals(comboStatut.getValue())) ? 1 : 0;

        boolean ok;
        if (selected == null) {
            CategorieProduit c = new CategorieProduit(nom, desc);
            c.setStatut(statut);
            ok = service.create(c);
        } else {
            selected.setNom(nom);
            selected.setDescription(desc);
            selected.setStatut(statut);
            ok = service.update(selected);
        }

        if (ok) {
            onCancel();
            loadData();
        } else {
            showError("Erreur lors de l'enregistrement.");
        }
    }

    @FXML
    private void onCancel() {
        if (formPanel != null) formPanel.setVisible(false);
        clearForm();
        selected = null;
    }

    @FXML
    private void onRetourBoutique() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/ShopView.fxml"));
            StackPane contentArea = (StackPane)
                    tableCategories.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("[CategorieProduitController] " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private boolean validate() {
        if (fieldNom == null || fieldNom.getText().trim().isEmpty()) {
            showError("Le nom est obligatoire."); return false;
        }
        if (lblFormError != null) lblFormError.setText("");
        return true;
    }

    private void showError(String msg) {
        if (lblFormError != null) lblFormError.setText("✗ " + msg);
    }

    private void clearForm() {
        if (fieldNom         != null) fieldNom.clear();
        if (fieldDescription != null) fieldDescription.clear();
        if (comboStatut      != null) comboStatut.setValue("Actif");
        if (lblFormError     != null) lblFormError.setText("");
    }

    private Button buildBtn(String icon, String color, String bg) {
        Button btn = new Button(icon);
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-color: " + color + "55;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;" +
            "-fx-padding: 5 10 5 10;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }
}
