package com.esports.controller;

import com.esports.model.CategorieProduit;
import com.esports.model.Produit;
import com.esports.service.AuthService;
import com.esports.service.CategorieProduitService;
import com.esports.service.ProduitService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * CONTROLLER — ShopPublicController.java
 * Vue front boutique : cartes produits avec images, filtres par catégorie, tri
 * READ ONLY — pas d'ajout/modification depuis le front
 */
public class ShopPublicController implements Initializable {

    // ── Navbar ─────────────────────────────────────────────────────
    @FXML private Label  lblConnectedUser;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnConnexion;
    @FXML private Button btnInscrire;
    @FXML private Button btnAdmin;

    // ── Filtres catégories (pills) ─────────────────────────────────
    @FXML private HBox boxCategories;

    // ── Tri ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> comboTri;

    // ── Grille produits ────────────────────────────────────────────
    @FXML private FlowPane flowProduits;

    // ── Message vide ───────────────────────────────────────────────
    @FXML private Label lblEmpty;

    // ── Services ──────────────────────────────────────────────────
    private final ProduitService          produitService = new ProduitService();
    private final CategorieProduitService catService     = new CategorieProduitService();

    private List<Produit> allProduits;
    private int selectedCategorieId = 0; // 0 = tous

    // ══════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshNavbar();
        setupTri();
        allProduits = produitService.findAll();
        buildCategoryPills();
        renderCards(allProduits);
    }

    // ── ComboBox tri ──────────────────────────────────────────────
    private void setupTri() {
        if (comboTri != null) {
            comboTri.setItems(FXCollections.observableArrayList(
                    "Par défaut", "Prix croissant", "Prix décroissant", "Nom A→Z"));
            comboTri.setValue("Par défaut");
            comboTri.setOnAction(e -> applyFilter());
        }
    }

    // ── Pills catégories ──────────────────────────────────────────
    private void buildCategoryPills() {
        if (boxCategories == null) return;
        boxCategories.getChildren().clear();

        // Pill "Tous"
        boxCategories.getChildren().add(buildPill("Tous", 0, true));

        // Pills par catégorie
        for (CategorieProduit cat : catService.findAll()) {
            boxCategories.getChildren().add(buildPill(cat.getNom(), cat.getId(), false));
        }
    }

    private Button buildPill(String text, int categorieId, boolean active) {
        Button pill = new Button(text);
        pill.setCursor(javafx.scene.Cursor.HAND);
        stylePill(pill, active);
        pill.setOnAction(e -> {
            selectedCategorieId = categorieId;
            // Réinitialiser tous les pills
            boxCategories.getChildren().forEach(node -> {
                if (node instanceof Button b) stylePill(b, false);
            });
            stylePill(pill, true);
            applyFilter();
        });
        return pill;
    }

    private void stylePill(Button pill, boolean active) {
        if (active) {
            pill.setStyle(
                "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 30px; -fx-padding: 7 22 7 22;" +
                "-fx-border-color: transparent; -fx-cursor: hand;"
            );
        } else {
            pill.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #c4b5fd; -fx-font-size: 13px;" +
                "-fx-background-radius: 30px; -fx-padding: 6 20 6 20;" +
                "-fx-border-color: rgba(139,92,246,0.4); -fx-border-width: 1px;" +
                "-fx-border-radius: 30px; -fx-cursor: hand;"
            );
        }
    }

    // ── Filtre + tri ──────────────────────────────────────────────
    private void applyFilter() {
        List<Produit> filtered = allProduits.stream()
                .filter(p -> selectedCategorieId == 0
                        || p.getIdCategoriesProduitId() == selectedCategorieId)
                .collect(Collectors.toList());

        String tri = comboTri != null ? comboTri.getValue() : "Par défaut";
        switch (tri) {
            case "Prix croissant"  -> filtered.sort(Comparator.comparingDouble(Produit::getPrix));
            case "Prix décroissant"-> filtered.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
            case "Nom A→Z"         -> filtered.sort(Comparator.comparing(Produit::getNom));
        }

        renderCards(filtered);
    }

    // ══════════════════════════════════════════════════════════════
    // RENDU DES CARTES
    // ══════════════════════════════════════════════════════════════

    private void renderCards(List<Produit> list) {
        if (flowProduits == null) return;
        flowProduits.getChildren().clear();

        boolean empty = list.isEmpty();
        if (lblEmpty != null) {
            lblEmpty.setVisible(empty);
            lblEmpty.setManaged(empty);
        }
        if (empty) return;

        for (Produit p : list) {
            flowProduits.getChildren().add(buildCard(p));
        }
    }

    private VBox buildCard(Produit p) {
        String gradientFallback =
            "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #be185d);" +
            "-fx-background-radius: 14px 14px 0 0;";

        // -- ImageView --
        ImageView imgView = new ImageView();
        imgView.setFitWidth(370);
        imgView.setFitHeight(220);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(370, 220);
        clip.setArcWidth(16); clip.setArcHeight(16);
        imgView.setClip(clip);

        // -- Conteneur image (declare en premier pour le listener) --
        StackPane imgContainer = new StackPane(imgView);
        imgContainer.setPrefSize(370, 220);
        imgContainer.setStyle(gradientFallback);

        // -- Chargement image --
        String src = p.getImage();
        if (src != null && !src.isBlank()) {
            try {
                Image img;
                String imgUrl = toImageUrl(src);
                if (imgUrl != null) {
                    img = new Image(imgUrl, 370, 220, false, true, true);
                } else {
                    img = null;
                }
                if (img != null) {
                    imgView.setImage(img);
                    // Listener : image echoue en async
                    img.errorProperty().addListener((obs, was, isErr) -> {
                        if (isErr) { imgView.setImage(null); imgContainer.setStyle(gradientFallback); }
                    });
                    if (img.isError()) { imgView.setImage(null); }
                }
            } catch (Exception ignored) {}
        }

        // -- Corps de la carte --
        VBox body = new VBox(8);
        body.setPadding(new javafx.geometry.Insets(16, 18, 18, 18));
        body.setStyle("-fx-background-color: rgba(15,10,30,0.95);");

        Label lblNom = new Label(p.getNom());
        lblNom.setStyle(
            "-fx-text-fill: white; -fx-font-size: 18px;" +
            "-fx-font-weight: bold; -fx-font-family: 'Arial Black';"
        );

        String desc = p.getDescription() != null && p.getDescription().length() > 55
                ? p.getDescription().substring(0, 55) + "..."
                : (p.getDescription() != null ? p.getDescription() : "");
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(330);

        Label lblPrix = new Label(String.valueOf((int) p.getPrix()));
        lblPrix.setStyle(
            "-fx-text-fill: #a855f7; -fx-font-size: 28px;" +
            "-fx-font-weight: bold; -fx-font-family: 'Arial Black';"
        );
        Label lblDT = new Label("DT");
        lblDT.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox prixBox = new HBox(4, lblPrix, lblDT);
        prixBox.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);

        Label lblStock = new Label(p.isDisponible() ? "\u25cf Disponible" : "X Rupture");
        lblStock.setStyle(p.isDisponible()
            ? "-fx-text-fill: #4ade80; -fx-font-size: 11px; -fx-font-weight: bold;"
            : "-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold;"
        );

        Button btnVoir = new Button("Voir plus");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setStyle(
            "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10px; -fx-padding: 11 0 11 0;" +
            "-fx-cursor: hand; -fx-border-color: transparent;"
        );
        btnVoir.setOnAction(e -> showDetail(p));
        VBox.setVgrow(btnVoir, javafx.scene.layout.Priority.NEVER);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        bottomRow.getChildren().addAll(prixBox, spacer, lblStock);

        body.getChildren().addAll(lblNom, lblDesc, bottomRow, btnVoir);

        // -- Assemblage --
        VBox card = new VBox(0, imgContainer, body);
        card.setPrefWidth(370);
        card.setStyle(
            "-fx-background-color: rgba(15,10,30,0.95);" +
            "-fx-border-color: rgba(139,92,246,0.25);" +
            "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
            "-fx-background-radius: 14px; -fx-cursor: hand;"
        );

        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: rgba(26,14,55,0.98);" +
            "-fx-border-color: #a855f7;" +
            "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
            "-fx-background-radius: 14px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.4), 20, 0.3, 0, 4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: rgba(15,10,30,0.95);" +
            "-fx-border-color: rgba(139,92,246,0.25);" +
            "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
            "-fx-background-radius: 14px; -fx-cursor: hand;"
        ));

        return card;
    }

    // ── Détail produit (popup) ─────────────────────────────────────
    private void showDetail(Produit p) {
        com.esports.utils.NexusDialog.showProduitDetail(p);
    }

    // ══════════════════════════════════════════════════════════════
    // NAVBAR — état connecté / déconnecté
    // ══════════════════════════════════════════════════════════════

    private void refreshNavbar() {
        boolean connected = AuthService.isLoggedIn();

        if (lblConnectedUser != null) {
            lblConnectedUser.setVisible(connected);
            lblConnectedUser.setManaged(connected);
            if (connected && AuthService.getCurrentUser() != null) {
                com.esports.model.User u = AuthService.getCurrentUser();
                lblConnectedUser.setText("👤 " + u.getNom() + " " + u.getPrenom());
            }
        }
        if (btnDeconnexion != null) {
            btnDeconnexion.setVisible(connected);
            btnDeconnexion.setManaged(connected);
        }
        if (btnConnexion != null) {
            btnConnexion.setVisible(!connected);
            btnConnexion.setManaged(!connected);
        }
        if (btnInscrire != null) {
            btnInscrire.setVisible(!connected);
            btnInscrire.setManaged(!connected);
        }
        if (btnAdmin != null) {
            boolean isAdmin = connected && AuthService.getCurrentUser() != null
                    && "admin".equalsIgnoreCase(AuthService.getCurrentUser().getRole());
            btnAdmin.setVisible(isAdmin);
            btnAdmin.setManaged(isAdmin);
        }
    }

    @FXML
    private void onLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setWidth(w);
            stage.setHeight(h);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogout() {
        AuthService.logout();
        refreshNavbar();
    }

    @FXML
    private void onAdminBackoffice() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/MainView.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) flowProduits.getScene().getWindow();
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

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setTitle("NexUS Gaming Arena");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String toImageUrl(String src) {
        if (src == null || src.isBlank()) return null;
        src = src.trim().replace("\\", "/");
        if (src.startsWith("http://") || src.startsWith("https://")) return src;
        if (src.startsWith("file:///")) return src;
        if (src.startsWith("file://"))  return src;
        if (src.length() > 1 && src.charAt(1) == ':') return "file:///" + src;
        if (src.startsWith("/")) return "file://" + src;
        return "file:///" + src;
    }


}
