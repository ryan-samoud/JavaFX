package com.esports.controller;

import com.esports.model.CategorieProduit;
import com.esports.model.Produit;
import com.esports.service.CategorieProduitService;
import com.esports.service.ProduitService;

import javafx.beans.property.SimpleStringProperty;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ShopController implements Initializable {

    // ── Compteurs ─────────────────────────────────────────────────
    @FXML private Label lblProduitCount;
    @FXML private Label lblCatCount;

    // ── Filtres ───────────────────────────────────────────────────
    @FXML private TextField              fieldSearch;
    @FXML private TextField              fieldSearchCat;
    @FXML private ComboBox<String>       comboStock;
    @FXML private ComboBox<CategorieProduit> comboCategorie;

    // ── Table catégories ──────────────────────────────────────────
    @FXML private TableView<CategorieProduit>        tableCategories;
    @FXML private TableColumn<CategorieProduit, Integer> colCatId;
    @FXML private TableColumn<CategorieProduit, String>  colCatNom;
    @FXML private TableColumn<CategorieProduit, String>  colCatDesc;
    @FXML private TableColumn<CategorieProduit, String>  colCatDate;
    @FXML private TableColumn<CategorieProduit, String>  colCatStatut;
    @FXML private TableColumn<CategorieProduit, String>  colCatActions;

    // ── Table produits ────────────────────────────────────────────
    @FXML private TableView<Produit>              tableProduits;
    @FXML private TableColumn<Produit, Integer>   colId;
    @FXML private TableColumn<Produit, String>    colNom;
    @FXML private TableColumn<Produit, String>    colCategorie;
    @FXML private TableColumn<Produit, String>    colPrix;
    @FXML private TableColumn<Produit, String>    colStock;
    @FXML private TableColumn<Produit, String>    colImage;
    @FXML private TableColumn<Produit, String>    colModel3d;
    @FXML private TableColumn<Produit, String>    colIdCat;
    @FXML private TableColumn<Produit, String>    colActions;

    // ── VBox racine (pour accéder au StackPane parent) ────────────
    @FXML private VBox rootVBox;

    // ── Services ──────────────────────────────────────────────────
    private final ProduitService          produitService = new ProduitService();
    private final CategorieProduitService catService     = new CategorieProduitService();

    private final ObservableList<Produit>          produits   = FXCollections.observableArrayList();
    private final ObservableList<CategorieProduit> categories = FXCollections.observableArrayList();

    private Produit          selectedProduit = null;
    private CategorieProduit selectedCat     = null;

    // ── Champs dialog Produit (créés programmatiquement) ──────────
    private StackPane overlayProduit;
    private Label     lblFormTitle;
    private TextField fieldNom;
    private TextArea  fieldDescription;
    private TextField fieldPrix;
    private ComboBox<String>           comboStockForm;
    private ComboBox<CategorieProduit> comboCategorieForm;
    private TextField fieldImage;
    private TextField fieldModel3d;
    private Label     lblFormError;
    private Button    btnConfirmer;
    private ImageView imgPreview;      // prévisualisation image dans le formulaire produit

    // ── Champs dialog Catégorie (créés programmatiquement) ────────
    private StackPane overlayCat;
    private Label     lblCatFormTitle;
    private TextField fieldCatNom;
    private TextArea  fieldCatDescription;
    private Label     lblCatFormError;
    private Button    btnCatConfirmer;

    // ══════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildDialogProduit();
        buildDialogCategorie();
        setupTableCategories();
        setupTableProduits();
        setupFilters();
        loadCategories();
        loadProduits();
    }

    // ══════════════════════════════════════════════════════════════
    // CONSTRUCTION DES DIALOGS PROGRAMMATIQUE
    // ══════════════════════════════════════════════════════════════

    private void buildDialogProduit() {
        // Champs
        fieldNom         = styledField("Ex: Souris Gaming");
        fieldDescription = styledArea("Description du produit…");
        fieldPrix        = styledField("ex: 29.99");
        fieldImage       = styledField("chemin ou URL image");
        fieldModel3d     = styledField("chemin modèle 3D");
        comboStockForm   = new ComboBox<>();
        comboStockForm.setItems(FXCollections.observableArrayList("dispo", "non_dispo"));
        comboStockForm.setValue("dispo");
        styleCombo(comboStockForm);
        comboCategorieForm = new ComboBox<>();
        styleCombo(comboCategorieForm);
        lblFormError = new Label();
        lblFormError.setWrapText(true);
        lblFormError.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");

        lblFormTitle = new Label("Ajouter un Nouveau Produit");
        lblFormTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        btnConfirmer = dialogPrimaryBtn("✔  Confirmer l'ajout");
        btnConfirmer.setOnAction(e -> onSave());
        Button btnAnnuler = dialogSecondaryBtn("Annuler");
        btnAnnuler.setOnAction(e -> hideOverlay(overlayProduit));

        // Layout
        HBox titleRow = new HBox(10, new Label("🛍️"), lblFormTitle);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        ((Label)titleRow.getChildren().get(0)).setStyle("-fx-font-size: 22px;");

        Rectangle sep = new Rectangle(); sep.setHeight(1);
        sep.setStyle("-fx-fill: rgba(139,92,246,0.25);");
        sep.widthProperty().bind(titleRow.widthProperty());

        HBox prixStock = new HBox(14,
            formGroup("Prix (DT) *", fieldPrix),
            formGroup("Stock *", comboStockForm));
        HBox.setHgrow(prixStock.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(prixStock.getChildren().get(1), Priority.ALWAYS);

        // ── Prévisualisation image ─────────────────────────────────
        imgPreview = new ImageView();
        imgPreview.setFitWidth(100);
        imgPreview.setFitHeight(70);
        imgPreview.setPreserveRatio(true);
        imgPreview.setSmooth(true);
        StackPane previewPane = new StackPane(imgPreview);
        previewPane.setPrefSize(100, 70);
        previewPane.setMinSize(100, 70);
        previewPane.setStyle(
            "-fx-background-color: rgba(139,92,246,0.08);" +
            "-fx-border-color: rgba(139,92,246,0.25);" +
            "-fx-border-width: 1px; -fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );
        Label lblNoImg = new Label("Aperçu");
        lblNoImg.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px;");
        previewPane.getChildren().add(lblNoImg); // affiché si pas d'image

        // Mise à jour en temps réel quand on saisit le chemin
        fieldImage.textProperty().addListener((obs, old, val) -> {
            lblNoImg.setVisible(false);
            String v = val.trim();
            if (v.isBlank()) { imgPreview.setImage(null); lblNoImg.setVisible(true); return; }
            try {
                String url1 = toImageUrl(v);
                if (url1 == null) { imgPreview.setImage(null); lblNoImg.setVisible(true); return; }
                Image img = new Image(url1, 100, 70, true, true, true);
                img.errorProperty().addListener((o2, w, err) -> {
                    if (err) { imgPreview.setImage(null); lblNoImg.setVisible(true); }
                });
                imgPreview.setImage(img);
            } catch (Exception ignored) { imgPreview.setImage(null); lblNoImg.setVisible(true); }
        });

        // Champ image + preview côte à côte
        VBox imgFieldGroup = formGroup("Image (chemin/URL)", fieldImage);
        HBox imgWithPreview = new HBox(10, imgFieldGroup, previewPane);
        imgWithPreview.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imgFieldGroup, Priority.ALWAYS);

        HBox catImage = new HBox(14,
            formGroup("Catégorie *", comboCategorieForm),
            imgWithPreview);
        HBox.setHgrow(catImage.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(catImage.getChildren().get(1), Priority.ALWAYS);

        HBox btnRow = new HBox(12, btnAnnuler, btnConfirmer);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16,
            titleRow, sep,
            formGroup("Nom du Produit *", fieldNom),
            formGroup("Description", fieldDescription),
            prixStock, catImage,
            formGroup("Modèle 3D (optionnel)", fieldModel3d),
            lblFormError, btnRow);
        card.setMaxWidth(580);
        card.setStyle(
            "-fx-background-color: #14112b;" +
            "-fx-border-color: rgba(139,92,246,0.45);" +
            "-fx-border-width: 1.5px; -fx-border-radius: 16px;" +
            "-fx-background-radius: 16px; -fx-padding: 32 36 28 36;" +
            "-fx-effect: dropshadow(gaussian,rgba(139,92,246,0.5),40,0.3,0,0);");

        overlayProduit = new StackPane(card);
        overlayProduit.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlayProduit.setVisible(false);
        overlayProduit.setManaged(false);
    }

    private void buildDialogCategorie() {
        fieldCatNom         = styledField("Nom de la catégorie");
        fieldCatDescription = styledArea("Description de la catégorie…");
        lblCatFormError = new Label();
        lblCatFormError.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");

        lblCatFormTitle = new Label("Ajouter une Catégorie");
        lblCatFormTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        btnCatConfirmer = dialogPrimaryBtn("✔  Confirmer l'ajout");
        btnCatConfirmer.setOnAction(e -> onSaveCat());
        Button btnAnnuler = dialogSecondaryBtn("Annuler");
        btnAnnuler.setOnAction(e -> hideOverlay(overlayCat));

        HBox titleRow = new HBox(10, new Label("🏷"), lblCatFormTitle);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        ((Label)titleRow.getChildren().get(0)).setStyle("-fx-font-size: 22px;");

        Rectangle sep = new Rectangle(); sep.setHeight(1);
        sep.setStyle("-fx-fill: rgba(139,92,246,0.25);");
        sep.widthProperty().bind(titleRow.widthProperty());

        HBox btnRow = new HBox(12, btnAnnuler, btnCatConfirmer);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16,
            titleRow, sep,
            formGroup("Nom *", fieldCatNom),
            formGroup("Description", fieldCatDescription),
            lblCatFormError, btnRow);
        card.setMaxWidth(460);
        card.setStyle(
            "-fx-background-color: #14112b;" +
            "-fx-border-color: rgba(139,92,246,0.45);" +
            "-fx-border-width: 1.5px; -fx-border-radius: 16px;" +
            "-fx-background-radius: 16px; -fx-padding: 32 36 28 36;" +
            "-fx-effect: dropshadow(gaussian,rgba(139,92,246,0.5),40,0.3,0,0);");

        overlayCat = new StackPane(card);
        overlayCat.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlayCat.setVisible(false);
        overlayCat.setManaged(false);
    }

    /** Injecte les overlays dans le StackPane contentArea du parent */
    private void injectOverlays() {
        if (rootVBox.getScene() == null) return;
        javafx.scene.Node parent = rootVBox.getParent();
        if (parent instanceof StackPane sp) {
            if (!sp.getChildren().contains(overlayProduit))
                sp.getChildren().add(overlayProduit);
            if (!sp.getChildren().contains(overlayCat))
                sp.getChildren().add(overlayCat);
        }
    }

    private void showOverlay(StackPane overlay) {
        injectOverlays();
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void hideOverlay(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════
    // TABLE CATÉGORIES
    // ══════════════════════════════════════════════════════════════

    private static final java.time.format.DateTimeFormatter CAT_DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private void setupTableCategories() {
        // ID
        if (colCatId  != null) colCatId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Nom
        if (colCatNom != null) colCatNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        // Description
        if (colCatDesc != null) colCatDesc.setCellValueFactory(
                d -> new SimpleStringProperty(
                    d.getValue().getDescription() != null ? d.getValue().getDescription() : ""));

        // Date création
        if (colCatDate != null) colCatDate.setCellValueFactory(d -> {
            var date = d.getValue().getDateCreation();
            return new SimpleStringProperty(date != null ? date.format(CAT_DATE_FMT) : "—");
        });

        // Statut — badge coloré
        if (colCatStatut != null) {
            colCatStatut.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null); return;
                    }
                    CategorieProduit c = getTableView().getItems().get(getIndex());
                    Label badge = new Label(c.isActive() ? "● Actif" : "○ Inactif");
                    badge.setStyle(c.isActive()
                        ? "-fx-text-fill:#4ade80;-fx-font-size:11px;-fx-font-weight:bold;" +
                          "-fx-background-color:rgba(74,222,128,0.12);" +
                          "-fx-border-color:rgba(74,222,128,0.3);-fx-border-width:1px;" +
                          "-fx-border-radius:20px;-fx-background-radius:20px;-fx-padding:3 10 3 10;"
                        : "-fx-text-fill:#f87171;-fx-font-size:11px;-fx-font-weight:bold;" +
                          "-fx-background-color:rgba(248,113,113,0.12);" +
                          "-fx-border-color:rgba(248,113,113,0.3);-fx-border-width:1px;" +
                          "-fx-border-radius:20px;-fx-background-radius:20px;-fx-padding:3 10 3 10;"
                    );
                    setGraphic(badge);
                }
            });
        }

        // Actions
        if (colCatActions != null) {
            colCatActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit   = buildBtn("=  Modifier", "#f59e0b", "#422006");
                private final Button btnDelete = buildIconBtn("X", "#f87171", "#450a0a");
                private final HBox   box       = new HBox(8, btnEdit, btnDelete);
                { box.setAlignment(Pos.CENTER_LEFT);
                  btnEdit.setOnAction(e -> {
                      CategorieProduit c = getTableView().getItems().get(getIndex());
                      boolean ok = com.esports.utils.NexusDialog.showConfirm(
                          "Modifier la cat\u00e9gorie",
                          "Modifier \"" + c.getNom() + "\" ?",
                          "Vous allez ouvrir le formulaire de modification."
                      );
                      if (ok) onEditCat(c);
                  });
                  btnDelete.setOnAction(e -> onDeleteCat(getTableView().getItems().get(getIndex()))); }
                @Override protected void updateItem(String i, boolean empty) {
                    super.updateItem(i, empty); setGraphic(empty ? null : box); }
            });
        }

        if (tableCategories != null) tableCategories.setItems(categories);
    }

    private void loadCategories() {
        List<CategorieProduit> cats = catService.findAll();
        categories.setAll(cats);
        if (lblCatCount != null) lblCatCount.setText(cats.size() + " catégorie(s)");

        // Alimenter combos
        ObservableList<CategorieProduit> catList = FXCollections.observableArrayList(cats);
        if (comboCategorieForm != null) {
            comboCategorieForm.setItems(catList);
            if (!catList.isEmpty()) comboCategorieForm.setValue(catList.get(0));
        }
        if (comboCategorie != null) {
            ObservableList<CategorieProduit> withAll = FXCollections.observableArrayList();
            withAll.add(new CategorieProduit(0, "Toutes les catégories", "", null, 1));
            withAll.addAll(cats);
            comboCategorie.setItems(withAll);
            if (comboCategorie.getValue() == null) comboCategorie.setValue(withAll.get(0));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TABLE PRODUITS
    // ══════════════════════════════════════════════════════════════

    private void setupTableProduits() {
        // ID
        if (colId    != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // ID Catégorie
        if (colIdCat != null) colIdCat.setCellValueFactory(
                d -> new SimpleStringProperty(String.valueOf(d.getValue().getIdCategoriesProduitId())));

        // Nom
        if (colNom   != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        // Catégorie (nom)
        if (colCategorie != null) colCategorie.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getNomCategorie()));

        // Prix formaté
        if (colPrix != null) colPrix.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getPrixFormate()));

        // Stock badge coloré
        if (colStock != null) {
            colStock.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                    Produit p = getTableView().getItems().get(getIndex());
                    Label badge = new Label(p.getStockLabel());
                    badge.setStyle(p.isDisponible()
                        ? "-fx-text-fill:#4ade80;-fx-font-size:11px;-fx-font-weight:bold;" +
                          "-fx-background-color:rgba(74,222,128,0.12);" +
                          "-fx-border-color:rgba(74,222,128,0.3);-fx-border-width:1px;" +
                          "-fx-border-radius:20px;-fx-background-radius:20px;-fx-padding:3 10 3 10;"
                        : "-fx-text-fill:#f87171;-fx-font-size:11px;-fx-font-weight:bold;" +
                          "-fx-background-color:rgba(248,113,113,0.12);" +
                          "-fx-border-color:rgba(248,113,113,0.3);-fx-border-width:1px;" +
                          "-fx-border-radius:20px;-fx-background-radius:20px;-fx-padding:3 10 3 10;"
                    );
                    setGraphic(badge);
                }
            });
        }

        // IMAGE — miniature réelle 60×45
        if (colImage != null) {
            colImage.setCellFactory(col -> new TableCell<>() {
                private final ImageView iv = new ImageView();
                {
                    iv.setFitWidth(60);
                    iv.setFitHeight(45);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null); return;
                    }
                    Produit p = getTableView().getItems().get(getIndex());
                    String src = p.getImage();
                    if (src != null && !src.isBlank()) {
                        try {
                            Image img;
                            if (src.startsWith("http://") || src.startsWith("https://")) {
                                img = new Image(src, 60, 45, true, true, true);
                            } else {
                                String url2 = toImageUrl(src);
                                if (url2 == null) { Label lbl2=new Label("—"); lbl2.setStyle("-fx-text-fill:#6b7280;"); setGraphic(lbl2); return; }
                                img = new Image(url2, 60, 45, true, true, true);
                            }
                            iv.setImage(img);
                            setGraphic(iv);
                        } catch (Exception ex) {
                            Label lbl = new Label("—");
                            lbl.setStyle("-fx-text-fill:#6b7280;");
                            setGraphic(lbl);
                        }
                    } else {
                        Label lbl = new Label("—");
                        lbl.setStyle("-fx-text-fill:#6b7280;");
                        setGraphic(lbl);
                    }
                }
            });
        }

        // MODÈLE 3D — affiche le nom du fichier ou "—"
        if (colModel3d != null) {
            colModel3d.setCellValueFactory(d -> {
                String m = d.getValue().getModel3d();
                if (m == null || m.isBlank()) return new SimpleStringProperty("—");
                // Afficher seulement le nom du fichier
                int idx = Math.max(m.lastIndexOf('/'), m.lastIndexOf('\\'));
                return new SimpleStringProperty(idx >= 0 ? m.substring(idx + 1) : m);
            });
        }

        // ACTIONS
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnEdit   = buildBtn("=  Modifier", "#f59e0b", "#422006");
                private final Button btnDelete = buildIconBtn("X", "#f87171", "#450a0a");
                private final HBox   box       = new HBox(8, btnEdit, btnDelete);
                { box.setAlignment(Pos.CENTER_LEFT);
                  btnEdit.setOnAction(e -> {
                      Produit p = getTableView().getItems().get(getIndex());
                      boolean ok = com.esports.utils.NexusDialog.showConfirm(
                          "Modifier le produit",
                          "Modifier \"" + p.getNom() + "\" ?",
                          "Vous allez ouvrir le formulaire de modification."
                      );
                      if (ok) onEdit(p);
                  });
                  btnDelete.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex()))); }
                @Override protected void updateItem(String i, boolean empty) {
                    super.updateItem(i, empty); setGraphic(empty ? null : box); }
            });
        }

        if (tableProduits != null) tableProduits.setItems(produits);
    }

    private void loadProduits() {
        produits.setAll(produitService.findAll());
        if (lblProduitCount != null) lblProduitCount.setText(produits.size() + " produit(s)");
    }

    // ══════════════════════════════════════════════════════════════
    // FILTRES
    // ══════════════════════════════════════════════════════════════

    private void setupFilters() {
        if (comboStock != null) {
            comboStock.setItems(FXCollections.observableArrayList("Tous", "Disponible", "En rupture"));
            comboStock.setValue("Tous");
            comboStock.setOnAction(e -> applyFilter());
        }
        if (fieldSearch    != null) fieldSearch.textProperty().addListener((o, a, b) -> applyFilter());
        if (comboCategorie != null) comboCategorie.setOnAction(e -> applyFilter());
        if (fieldSearchCat != null) fieldSearchCat.textProperty().addListener((o, a, b) -> filterCats());
    }

    private void applyFilter() {
        String q     = fieldSearch     != null ? fieldSearch.getText().trim().toLowerCase() : "";
        String stock = comboStock      != null ? comboStock.getValue() : "Tous";
        CategorieProduit cat = comboCategorie != null ? comboCategorie.getValue() : null;
        ObservableList<Produit> filtered = FXCollections.observableArrayList();
        for (Produit p : produitService.findAll()) {
            boolean mNom   = q.isEmpty() || p.getNom().toLowerCase().contains(q);
            boolean mStock = "Tous".equals(stock)
                || ("Disponible".equals(stock) && p.isDisponible())
                || ("En rupture".equals(stock) && !p.isDisponible());
            boolean mCat   = cat == null || cat.getId() == 0 || p.getIdCategoriesProduitId() == cat.getId();
            if (mNom && mStock && mCat) filtered.add(p);
        }
        produits.setAll(filtered);
        if (lblProduitCount != null) lblProduitCount.setText(produits.size() + " produit(s)");
    }

    private void filterCats() {
        String q = fieldSearchCat != null ? fieldSearchCat.getText().trim().toLowerCase() : "";
        List<CategorieProduit> all = catService.findAll();
        categories.setAll(q.isEmpty() ? all :
            all.stream().filter(c -> c.getNom().toLowerCase().contains(q)).toList());
    }

    // ══════════════════════════════════════════════════════════════
    // ACTIONS PRODUITS
    // ══════════════════════════════════════════════════════════════

    @FXML private void onAddProduit() {
        selectedProduit = null;
        clearProduitForm();
        lblFormTitle.setText("Ajouter un Nouveau Produit");
        btnConfirmer.setText("✔  Confirmer l'ajout");
        showOverlay(overlayProduit);
    }

    private void onEdit(Produit p) {
        selectedProduit = p;
        lblFormTitle.setText("Modifier : " + p.getNom());
        btnConfirmer.setText("💾  Enregistrer les modifications");
        fieldNom.setText(p.getNom());
        fieldDescription.setText(p.getDescription() != null ? p.getDescription() : "");
        fieldImage.setText(p.getImage() != null ? p.getImage() : "");
        fieldPrix.setText(String.valueOf(p.getPrix()));
        comboStockForm.setValue(p.getStock());
        fieldModel3d.setText(p.getModel3d() != null ? p.getModel3d() : "");
        comboCategorieForm.getItems().stream()
            .filter(c -> c.getId() == p.getIdCategoriesProduitId())
            .findFirst().ifPresent(comboCategorieForm::setValue);
        showOverlay(overlayProduit);
    }

    private void onDelete(Produit p) {
        boolean confirmed = com.esports.utils.NexusDialog.showConfirm(
            "Supprimer le produit",
            "Supprimer \"" + p.getNom() + "\" ?",
            "Cette action est irréversible."
        );
        if (confirmed)
            if (produitService.delete(p.getId())) loadProduits();
    }

    @FXML private void onSave() {
        if (!validateProduit()) return;
        String nom    = fieldNom.getText().trim();
        String desc   = fieldDescription.getText().trim();
        String image  = fieldImage.getText().trim();
        double prix   = Double.parseDouble(fieldPrix.getText().trim().replace(",", "."));
        String stock  = comboStockForm.getValue();
        String m3d    = fieldModel3d.getText().trim();
        int    catId  = comboCategorieForm.getValue().getId();
        boolean ok;
        if (selectedProduit == null) {
            ok = produitService.create(new Produit(nom, desc, image, prix, stock, m3d, catId));
        } else {
            selectedProduit.setNom(nom); selectedProduit.setDescription(desc);
            selectedProduit.setImage(image); selectedProduit.setPrix(prix);
            selectedProduit.setStock(stock); selectedProduit.setModel3d(m3d);
            selectedProduit.setIdCategoriesProduitId(catId);
            ok = produitService.update(selectedProduit);
        }
        if (ok) { hideOverlay(overlayProduit); clearProduitForm(); loadProduits(); }
        else     lblFormError.setText("✗ Erreur lors de l'enregistrement.");
    }

    @FXML private void onCancelForm() { hideOverlay(overlayProduit); clearProduitForm(); }

    // ══════════════════════════════════════════════════════════════
    // ACTIONS CATÉGORIES
    // ══════════════════════════════════════════════════════════════

    @FXML private void onAddCategorie() {
        selectedCat = null;
        clearCatForm();
        lblCatFormTitle.setText("Ajouter une Catégorie");
        btnCatConfirmer.setText("✔  Confirmer l'ajout");
        showOverlay(overlayCat);
    }

    private void onEditCat(CategorieProduit c) {
        selectedCat = c;
        lblCatFormTitle.setText("Modifier : " + c.getNom());
        btnCatConfirmer.setText("💾  Enregistrer");
        fieldCatNom.setText(c.getNom());
        fieldCatDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        showOverlay(overlayCat);
    }

    private void onDeleteCat(CategorieProduit c) {
        boolean confirmed = com.esports.utils.NexusDialog.showConfirm(
            "Supprimer la catégorie",
            "Supprimer \"" + c.getNom() + "\" ?",
            "Les produits associés perdront leur catégorie."
        );
        if (confirmed)
            if (catService.delete(c.getId())) { loadCategories(); loadProduits(); }
    }

    @FXML private void onSaveCat() {
        lblCatFormError.setText("");

        // ── Nom obligatoire ───────────────────────────────────────
        String nom = fieldCatNom.getText().trim();
        if (nom.isEmpty()) {
            lblCatFormError.setText("✗ Le nom de la catégorie est obligatoire.");
            fieldCatNom.requestFocus();
            return;
        }
        if (nom.length() < 2) {
            lblCatFormError.setText("✗ Le nom doit contenir au moins 2 caractères.");
            fieldCatNom.requestFocus();
            return;
        }

        // ── Caractères spéciaux interdits (chiffres seuls, etc.) ──
        if (nom.matches("^[0-9]+$")) {
            lblCatFormError.setText("✗ Le nom ne peut pas être composé uniquement de chiffres.");
            fieldCatNom.requestFocus();
            return;
        }


        // ── Description catégorie : obligatoire, min 10, max 300 caractères ──
        String descCat = fieldCatDescription.getText().trim();
        if (descCat.isEmpty()) {
            lblCatFormError.setText("✗ La description est obligatoire.");
            fieldCatDescription.requestFocus();
            return;
        }
        if (descCat.length() < 10) {
            lblCatFormError.setText("✗ La description doit contenir au moins 10 caractères (actuellement : " + descCat.length() + ").");
            fieldCatDescription.requestFocus();
            return;
        }
        if (descCat.length() > 300) {
            lblCatFormError.setText("✗ La description ne doit pas dépasser 300 caractères (actuellement : " + descCat.length() + ").");
            fieldCatDescription.requestFocus();
            return;
        }
        // ── Test d'unicité ────────────────────────────────────────
        // Une catégorie est un doublon si elle a le même nom (insensible à la casse).
        int excludeId = (selectedCat != null) ? selectedCat.getId() : -1;
        if (catService.existsByNom(nom, excludeId)) {
            lblCatFormError.setText("✗ Une catégorie nommée \"" + nom + "\" existe déjà. Choisissez un nom différent.");
            fieldCatNom.requestFocus();
            return;
        }

        // ── Enregistrement ────────────────────────────────────────
        String desc = fieldCatDescription.getText().trim();
        boolean ok;
        if (selectedCat == null) {
            ok = catService.create(new CategorieProduit(nom, desc));
        } else {
            selectedCat.setNom(nom);
            selectedCat.setDescription(desc);
            ok = catService.update(selectedCat);
        }
        if (ok) { hideOverlay(overlayCat); clearCatForm(); loadCategories(); loadProduits(); }
        else     lblCatFormError.setText("✗ Erreur lors de l'enregistrement.");
    }

    @FXML private void onCancelCatForm() { hideOverlay(overlayCat); clearCatForm(); }

    // ══════════════════════════════════════════════════════════════
    // HELPERS — VALIDATION
    // ══════════════════════════════════════════════════════════════

    private boolean validateProduit() {
        lblFormError.setText("");

        // ── Champs obligatoires ───────────────────────────────────
        String nom = fieldNom.getText().trim();
        if (nom.isEmpty()) {
            lblFormError.setText("✗ Le nom du produit est obligatoire.");
            fieldNom.requestFocus();
            return false;
        }
        if (nom.length() < 2) {
            lblFormError.setText("✗ Le nom doit contenir au moins 2 caractères.");
            fieldNom.requestFocus();
            return false;
        }

        // ── Description : obligatoire, min 10, max 500 caractères ────
        String desc = fieldDescription.getText().trim();
        if (desc.isEmpty()) {
            lblFormError.setText("✗ La description est obligatoire.");
            fieldDescription.requestFocus();
            return false;
        }
        if (desc.length() < 10) {
            lblFormError.setText("✗ La description doit contenir au moins 10 caractères (actuellement : " + desc.length() + ").");
            fieldDescription.requestFocus();
            return false;
        }
        if (desc.length() > 500) {
            lblFormError.setText("✗ La description ne doit pas dépasser 500 caractères (actuellement : " + desc.length() + ").");
            fieldDescription.requestFocus();
            return false;
        }

        // ── Prix : obligatoire + type numérique + valeur positive ─
        String prixStr = fieldPrix.getText().trim();
        if (prixStr.isEmpty()) {
            lblFormError.setText("✗ Le prix est obligatoire.");
            fieldPrix.requestFocus();
            return false;
        }
        double prix;
        try {
            prix = Double.parseDouble(prixStr.replace(",", "."));
        } catch (NumberFormatException e) {
            lblFormError.setText("✗ Prix invalide — entrez un nombre (ex: 29.99).");
            fieldPrix.requestFocus();
            return false;
        }
        if (prix < 0) {
            lblFormError.setText("✗ Le prix ne peut pas être négatif.");
            fieldPrix.requestFocus();
            return false;
        }
        if (prix > 99999.99) {
            lblFormError.setText("✗ Le prix semble trop élevé (max 99 999.99 DT).");
            fieldPrix.requestFocus();
            return false;
        }

        // ── Catégorie obligatoire ─────────────────────────────────
        if (comboCategorieForm.getValue() == null) {
            lblFormError.setText("✗ Veuillez sélectionner une catégorie.");
            comboCategorieForm.requestFocus();
            return false;
        }

        // ── Stock obligatoire ─────────────────────────────────────
        if (comboStockForm.getValue() == null) {
            lblFormError.setText("✗ Veuillez indiquer le statut du stock.");
            comboStockForm.requestFocus();
            return false;
        }

        // ── Test d'unicité ────────────────────────────────────────
        // Un produit est considéré doublon si même nom (insensible à la casse),
        // même prix ET même catégorie.
        int excludeId = (selectedProduit != null) ? selectedProduit.getId() : -1;
        int catId = comboCategorieForm.getValue().getId();
        if (produitService.existsByNomPrixCategorie(nom, prix, catId, excludeId)) {
            lblFormError.setText("✗ Doublon détecté : un produit avec ce nom, ce prix et cette catégorie existe déjà.");
            return false;
        }

        return true;
    }

    private void clearProduitForm() {
        fieldNom.clear(); fieldDescription.clear(); fieldImage.clear();
        fieldPrix.clear(); fieldModel3d.clear(); lblFormError.setText("");
        comboStockForm.setValue("dispo");
        if (!comboCategorieForm.getItems().isEmpty())
            comboCategorieForm.setValue(comboCategorieForm.getItems().get(0));
    }

    private void clearCatForm() {
        fieldCatNom.clear(); fieldCatDescription.clear(); lblCatFormError.setText("");
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS — UI BUILDERS
    // ══════════════════════════════════════════════════════════════

    private VBox formGroup(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-font-weight: bold;");
        VBox g = new VBox(6, lbl, field);
        VBox.setVgrow(g, Priority.NEVER);
        HBox.setHgrow(g, Priority.ALWAYS);
        return g;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #4b5563; -fx-border-color: rgba(139,92,246,0.3);" +
                    "-fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-padding: 9 12 9 12; -fx-font-size: 13px;");
        return tf;
    }

    private TextArea styledArea(String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefHeight(70);
        ta.setWrapText(true);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #4b5563; -fx-border-color: rgba(139,92,246,0.3);" +
                    "-fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-padding: 9 12 9 12; -fx-font-size: 13px; -fx-control-inner-background: #0d0b1e;");
        return ta;
    }

    private <T> void styleCombo(ComboBox<T> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white;" +
                    "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 1px;" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-padding: 4 10 4 10; -fx-font-size: 13px;");
    }

    private Button dialogPrimaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                     "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                     "-fx-background-radius: 10px; -fx-padding: 11 32 11 32;" +
                     "-fx-cursor: hand; -fx-border-color: transparent;" +
                     "-fx-effect: dropshadow(gaussian,rgba(168,85,247,0.5),12,0.3,0,2);");
        return btn;
    }

    private Button dialogSecondaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #9ca3af;" +
                     "-fx-font-size: 13px; -fx-font-weight: bold;" +
                     "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1px;" +
                     "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                     "-fx-padding: 11 28 11 28; -fx-cursor: hand;");
        return btn;
    }

    private Button buildBtn(String text, String color, String bg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + color + ";" +
                     "-fx-font-size:12px; -fx-font-weight:bold; -fx-background-radius:7px;" +
                     "-fx-border-color:" + color + "55; -fx-border-width:1px;" +
                     "-fx-border-radius:7px; -fx-padding:6 14 6 14; -fx-cursor:hand;");
        return btn;
    }

    private Button buildIconBtn(String icon, String color, String bg) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + color + ";" +
                     "-fx-font-size:14px; -fx-background-radius:7px;" +
                     "-fx-border-color:" + color + "55; -fx-border-width:1px;" +
                     "-fx-border-radius:7px; -fx-padding:5 10 5 10; -fx-cursor:hand;");
        return btn;
    }
    // ── Helper : convertit un chemin ou URL en URL valide pour Image ──────
    // Gère les cas : URL http, chemin absolu Windows/Linux, path déjà préfixé file:///
    private static String toImageUrl(String src) {
        if (src == null || src.isBlank()) return null;
        src = src.trim().replace("\\", "/");
        if (src.startsWith("http://") || src.startsWith("https://")) return src;
        if (src.startsWith("file:///")) return src;          // déjà correct
        if (src.startsWith("file://"))  return src;          // idem
        // chemin absolu Windows (C:/...) ou Linux (/home/...)
        if (src.length() > 1 && src.charAt(1) == ':') return "file:///" + src;  // Windows
        if (src.startsWith("/")) return "file://" + src;     // Linux/Mac
        return "file:///" + src;                              // relatif → essai
    }


}
