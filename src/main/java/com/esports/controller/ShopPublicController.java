package com.esports.controller;


import com.esports.model.CategorieProduit;
import com.esports.model.Produit;
import com.esports.service.AuthService;
import com.esports.service.CategorieProduitService;
import com.esports.service.PanierService;
import com.esports.service.PanierSession;
import com.esports.service.ProduitService;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.esports.service.VisionService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import java.io.File;

public class ShopPublicController implements Initializable {

    @FXML private Label  lblConnectedUser;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnConnexion;
    @FXML private Button btnInscrire;
    @FXML private Button btnAdmin;
    @FXML private Label  lblPanierCount;
    @FXML private HBox   boxCategories;
    @FXML private ComboBox<String> comboTri;
    @FXML private FlowPane flowProduits;
    @FXML private Label lblEmpty;
    @FXML private HBox hboxLoading;

    private final ProduitService          produitService = new ProduitService();
    private final CategorieProduitService catService     = new CategorieProduitService();
    private final PanierService           panierService  = new PanierService();
    private final VisionService           visionService  = new VisionService();

    private List<Produit> allProduits;

    // Garde la dernière liste affichée (résultats vision ou tous les produits)
    private List<Produit> currentDisplayList;

    // true = on est en mode résultats vision, false = mode normal
    private boolean modeRecherche = false;

    private int selectedCategorieId = 0;
    private int panierId = 0;

    static {
        System.out.println("[DEBUG] ShopPublicController CLASS LOADED");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[DEBUG] initialize DEBUT");
        try {
            int userId = AuthService.isLoggedIn() ? AuthService.getCurrentUser().getId() : 0;
            System.out.println("[PANIER] userId=" + userId);
            var panier = panierService.getOrCreatePanier(userId);
            if (panier != null) {
                panierId = panier.getId();
                PanierSession.setPanierId(panierId);
                System.out.println("[PANIER] panierId=" + panierId);
            } else {
                System.out.println("[PANIER] panier null, force creation...");
                panierId = panierService.forceCreerPanier(userId);
                PanierSession.setPanierId(panierId);
                System.out.println("[PANIER] panierId force=" + panierId);
            }
        } catch (Exception ex) {
            System.out.println("[PANIER] EXCEPTION initialize: " + ex.getMessage());
            ex.printStackTrace();
        }
        refreshNavbar();
        setupTri();
        allProduits = produitService.findAll();
        currentDisplayList = new ArrayList<>(allProduits);
        buildCategoryPills();
        renderCards(allProduits);
        updatePanierBadge();
    }

    // ══════════════════════════════════════════════════════════════
    // RECHERCHE PAR IMAGE
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void onRechercherParImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de produit");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );

        Stage stage = (Stage) flowProduits.getScene().getWindow();
        File imageFile = fileChooser.showOpenDialog(stage);
        if (imageFile == null) return;

        // Snapshot immédiat de allProduits pour éviter toute modification concurrente
        final List<Produit> produitsPourRecherche = new ArrayList<>(allProduits);

        setLoadingState(true);

        Task<List<Produit>> task = new Task<>() {
            @Override
            protected List<Produit> call() throws Exception {
                System.out.println("[VISION] Fichier: " + imageFile.getName());
                List<String> labels = visionService.analyserImage(imageFile);
                System.out.println("[VISION] Labels retournes: " + labels);

                if (labels.isEmpty()) {
                    System.out.println("[VISION] Aucun label — API probablement pas configuree");
                    return new ArrayList<>();
                }

                List<Produit> resultats = visionService.chercherProduitsParLabels(labels, produitsPourRecherche);
                System.out.println("[VISION] Resultats matching: " + resultats.size() + " produit(s)");
                resultats.forEach(p -> System.out.println("[VISION]   -> " + p.getId() + " | " + p.getNom()));
                return resultats;
            }
        };

        task.setOnSucceeded(workerStateEvent -> {
            setLoadingState(false);

            // getValue() sur le thread JavaFX — aucun risque de race condition
            List<Produit> resultats = task.getValue();

            System.out.println("[VISION] setOnSucceeded — " + resultats.size() + " produit(s) a afficher");

            if (resultats.isEmpty()) {
                modeRecherche = false;
                showNotifRecherche("❌ Aucun produit correspondant trouvé.", false);
                // Reafficher tous les produits sans toucher au filtre catégorie
                renderCards(allProduits);
            } else {
                modeRecherche = true;
                // Stocker pour que applyFilter() ne l'écrase pas pendant qu'on affiche
                currentDisplayList = new ArrayList<>(resultats);
                showNotifRecherche("✅ " + resultats.size() + " produit(s) trouvé(s) !", true);
                // Appel direct et synchrone sur le thread JavaFX
                renderCards(currentDisplayList);
            }
        });

        task.setOnFailed(workerStateEvent -> {
            setLoadingState(false);
            Throwable ex = task.getException();
            System.out.println("[VISION] ERREUR Task: " + (ex != null ? ex.getMessage() : "inconnue"));
            if (ex != null) ex.printStackTrace();
            showNotifRecherche("❌ Erreur lors de l'analyse de l'image.", false);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onResetRecherche() {
        modeRecherche = false;
        selectedCategorieId = 0;
        // Remettre la pill "Tous" active visuellement
        if (boxCategories != null) {
            boxCategories.getChildren().forEach(node -> {
                if (node instanceof Button b) stylePill(b, b.getText().equals("Tous"));
            });
        }
        currentDisplayList = new ArrayList<>(allProduits);
        renderCards(allProduits);
        showNotifRecherche("↺ Tous les produits affichés", true);
    }

    private void setLoadingState(boolean loading) {
        if (hboxLoading != null) {
            hboxLoading.setVisible(loading);
            hboxLoading.setManaged(loading);
        }
        flowProduits.setOpacity(loading ? 0.4 : 1.0);
        flowProduits.setMouseTransparent(loading);
    }

    // ══════════════════════════════════════════════════════════════
    // PANIER
    // ══════════════════════════════════════════════════════════════

    private void updatePanierBadge() {
        if (lblPanierCount != null) {
            int nb = panierService.getNombreArticles(panierId);
            lblPanierCount.setText(nb > 0 ? String.valueOf(nb) : "");
            lblPanierCount.setVisible(nb > 0);
            lblPanierCount.setManaged(nb > 0);
        }
    }

    @FXML
    private void onOpenPanier() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/PanierView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("Mon Panier");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ajouterAuPanier(Produit p) {
        System.out.println("[PANIER] Ajout produit id=" + p.getId() + " panierId=" + panierId + " prix=" + p.getPrix());
        panierService.ajouterItem(panierId, p.getId(), p.getPrix());
        updatePanierBadge();
        showNexusNotification(p.getNom());
    }

    private void showNexusNotification(String nomProduit) {
        Stage notif = new Stage();
        notif.initStyle(StageStyle.TRANSPARENT);
        notif.initModality(Modality.NONE);

        Stage mainStage = (Stage) flowProduits.getScene().getWindow();

        Label icon = new Label("🛒");
        icon.setStyle("-fx-font-size: 26px;");

        Label lblMsg = new Label("✓  " + nomProduit + " ajouté !");
        lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblSub = new Label("Cliquez sur 🛒 pour voir votre panier");
        lblSub.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11px;");

        VBox texts = new VBox(4, lblMsg, lblSub);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: transparent;" +
                        "-fx-padding: 0 2 0 2;"
        );
        btnClose.setOnAction(e -> notif.close());

        HBox root = new HBox(12, icon, texts, btnClose);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setPrefWidth(340);
        root.setStyle(
                "-fx-background-color: #1a0a3e;" +
                        "-fx-border-color: #a855f7;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.7), 24, 0.4, 0, 4);"
        );

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        notif.setScene(scene);

        notif.setX(mainStage.getX() + mainStage.getWidth() - 370);
        notif.setY(mainStage.getY() + mainStage.getHeight() - 110);
        notif.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> notif.close());
        pause.play();
    }

    private void showDetailAvecPanier(Produit p) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(440);
        imgView.setFitHeight(260);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(440, 260);
        clip.setArcWidth(16); clip.setArcHeight(16);
        imgView.setClip(clip);

        StackPane imgContainer = new StackPane(imgView);
        imgContainer.setPrefSize(440, 260);
        imgContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #be185d);" +
                        "-fx-background-radius: 14px 14px 0 0;"
        );
        if (p.getImage() != null && !p.getImage().isBlank()) {
            try {
                String url = toImageUrl(p.getImage());
                if (url != null) imgView.setImage(new Image(url, 440, 260, false, true, true));
            } catch (Exception ignored) {}
        }

        Label lblNom = new Label(p.getNom());
        lblNom.setStyle(
                "-fx-text-fill: white; -fx-font-size: 22px;" +
                        "-fx-font-weight: bold; -fx-font-family: 'Arial Black';"
        );

        Label lblDesc = new Label(p.getDescription() != null ? p.getDescription() : "");
        lblDesc.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(400);

        Label lblPrix = new Label(String.format("%.0f DT", p.getPrix()));
        lblPrix.setStyle(
                "-fx-text-fill: #a855f7; -fx-font-size: 32px;" +
                        "-fx-font-weight: bold; -fx-font-family: 'Arial Black';"
        );

        Label lblStock = new Label(p.isDisponible() ? "● Disponible" : "✕ Rupture de stock");
        lblStock.setStyle(p.isDisponible()
                ? "-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;"
        );

        Button btnAjouter = new Button("🛒  Ajouter au panier");
        btnAjouter.setMaxWidth(Double.MAX_VALUE);
        btnAjouter.setStyle(
                "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                        "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-padding: 13 0 13 0; -fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        );
        btnAjouter.setOnAction(e -> { ajouterAuPanier(p); dialog.close(); });

        Button btnFermer = new Button("✕  Fermer");
        btnFermer.setMaxWidth(Double.MAX_VALUE);
        btnFermer.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                        "-fx-border-color: rgba(139,92,246,0.4); -fx-border-width: 1px;" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-font-size: 13px; -fx-padding: 10 0 10 0; -fx-cursor: hand;"
        );
        btnFermer.setOnAction(e -> dialog.close());

        VBox body = new VBox(14, lblNom, lblDesc, lblPrix, lblStock, btnAjouter, btnFermer);
        body.setPadding(new Insets(22, 24, 24, 24));
        body.setStyle("-fx-background-color: rgba(13,8,30,0.98); -fx-background-radius: 0 0 14px 14px;");

        VBox dialogRoot = new VBox(0, imgContainer, body);
        dialogRoot.setPrefWidth(440);
        dialogRoot.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #a855f7;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.6), 30, 0.3, 0, 6);"
        );

        Scene scene = new Scene(dialogRoot);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);

        Stage mainStage = (Stage) flowProduits.getScene().getWindow();
        dialog.setX(mainStage.getX() + (mainStage.getWidth() - 440) / 2);
        dialog.setY(mainStage.getY() + (mainStage.getHeight() - 560) / 2);
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // CARTES PRODUITS
    // ══════════════════════════════════════════════════════════════

    private void renderCards(List<Produit> list) {
        if (flowProduits == null) return;

        // Toujours opérer sur une copie locale pour éviter les modifications externes
        List<Produit> snapshot = new ArrayList<>(list);

        flowProduits.getChildren().clear();

        boolean empty = snapshot.isEmpty();
        if (lblEmpty != null) {
            lblEmpty.setVisible(empty);
            lblEmpty.setManaged(empty);
        }
        if (empty) return;

        System.out.println("[RENDER] Affichage de " + snapshot.size() + " carte(s):");
        for (Produit p : snapshot) {
            System.out.println("[RENDER]   -> id=" + p.getId() + " nom=" + p.getNom());
            flowProduits.getChildren().add(buildCard(p));
        }
    }

    private VBox buildCard(Produit p) {
        String gradientFallback =
                "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #be185d);" +
                        "-fx-background-radius: 14px 14px 0 0;";

        ImageView imgView = new ImageView();
        imgView.setFitWidth(370);
        imgView.setFitHeight(220);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(370, 220);
        clip.setArcWidth(16); clip.setArcHeight(16);
        imgView.setClip(clip);

        StackPane imgContainer = new StackPane(imgView);
        imgContainer.setPrefSize(370, 220);
        imgContainer.setStyle(gradientFallback);

        String src = p.getImage();
        if (src != null && !src.isBlank()) {
            try {
                String imgUrl = toImageUrl(src);
                if (imgUrl != null) {
                    Image img = new Image(imgUrl, 370, 220, false, true, true);
                    imgView.setImage(img);
                    img.errorProperty().addListener((obs, was, isErr) -> {
                        if (isErr) { imgView.setImage(null); imgContainer.setStyle(gradientFallback); }
                    });
                    if (img.isError()) imgView.setImage(null);
                }
            } catch (Exception ignored) {}
        }

        VBox body = new VBox(8);
        body.setPadding(new Insets(16, 18, 18, 18));
        body.setStyle("-fx-background-color: rgba(15,10,30,0.95);");

        Label lblNom = new Label(p.getNom());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 18px;" +
                "-fx-font-weight: bold; -fx-font-family: 'Arial Black';");

        String desc = p.getDescription() != null && p.getDescription().length() > 55
                ? p.getDescription().substring(0, 55) + "..."
                : (p.getDescription() != null ? p.getDescription() : "");
        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxWidth(330);

        Label lblPrix = new Label(String.valueOf((int) p.getPrix()));
        lblPrix.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 28px;" +
                "-fx-font-weight: bold; -fx-font-family: 'Arial Black';");
        Label lblDT = new Label("DT");
        lblDT.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox prixBox = new HBox(4, lblPrix, lblDT);
        prixBox.setAlignment(Pos.BASELINE_LEFT);

        Label lblStock = new Label(p.isDisponible() ? "\u25cf Disponible" : "✕ Rupture");
        lblStock.setStyle(p.isDisponible()
                ? "-fx-text-fill: #4ade80; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomRow.getChildren().addAll(prixBox, spacer, lblStock);

        Button btnVoir = new Button("👁  Voir plus");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #c4b5fd; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(139,92,246,0.5); -fx-border-width: 1.5px;" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-padding: 9 0 9 0; -fx-cursor: hand;"
        );
        // Capture explicite du produit pour éviter toute ambiguité de closure
        final Produit produitCapture = p;
        btnVoir.setOnAction(e -> showDetailAvecPanier(produitCapture));

        Button btnAjouter = new Button("🛒  Ajouter au panier");
        btnAjouter.setMaxWidth(Double.MAX_VALUE);
        btnAjouter.setStyle(
                "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                        "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-padding: 11 0 11 0;" +
                        "-fx-cursor: hand; -fx-border-color: transparent;"
        );
        btnAjouter.setOnAction(e -> ajouterAuPanier(produitCapture));

        body.getChildren().addAll(lblNom, lblDesc, bottomRow, btnVoir, btnAjouter);

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

    // ══════════════════════════════════════════════════════════════
    // TRI / FILTRE
    // ══════════════════════════════════════════════════════════════

    private void setupTri() {
        if (comboTri != null) {
            comboTri.setItems(FXCollections.observableArrayList(
                    "Par défaut", "Prix croissant", "Prix décroissant", "Nom A→Z"));
            comboTri.setValue("Par défaut");
            comboTri.setOnAction(e -> applyFilter());
        }
    }

    private void buildCategoryPills() {
        if (boxCategories == null) return;
        boxCategories.getChildren().clear();
        boxCategories.getChildren().add(buildPill("Tous", 0, true));
        for (CategorieProduit cat : catService.findAll()) {
            boxCategories.getChildren().add(buildPill(cat.getNom(), cat.getId(), false));
        }
    }

    private Button buildPill(String text, int categorieId, boolean active) {
        Button pill = new Button(text);
        pill.setCursor(javafx.scene.Cursor.HAND);
        stylePill(pill, active);
        pill.setOnAction(e -> {
            // Cliquer sur une catégorie quitte le mode recherche
            modeRecherche = false;
            selectedCategorieId = categorieId;
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

    private void applyFilter() {
        // En mode recherche, le filtre catégorie et le tri ne s'appliquent pas
        if (modeRecherche) return;

        List<Produit> filtered = allProduits.stream()
                .filter(p -> selectedCategorieId == 0
                        || p.getIdCategoriesProduitId() == selectedCategorieId)
                .collect(Collectors.toList());

        String tri = comboTri != null ? comboTri.getValue() : "Par défaut";
        switch (tri) {
            case "Prix croissant"   -> filtered.sort(Comparator.comparingDouble(Produit::getPrix));
            case "Prix décroissant" -> filtered.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
            case "Nom A→Z"          -> filtered.sort(Comparator.comparing(Produit::getNom));
        }

        currentDisplayList = new ArrayList<>(filtered);
        renderCards(currentDisplayList);
    }

    // ══════════════════════════════════════════════════════════════
    // NAVBAR
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
        if (btnDeconnexion != null) { btnDeconnexion.setVisible(connected);  btnDeconnexion.setManaged(connected); }
        if (btnConnexion   != null) { btnConnexion.setVisible(!connected);   btnConnexion.setManaged(!connected); }
        if (btnInscrire    != null) { btnInscrire.setVisible(!connected);    btnInscrire.setManaged(!connected); }
        if (btnAdmin != null) {
            boolean isAdmin = connected && AuthService.getCurrentUser() != null
                    && "admin".equalsIgnoreCase(AuthService.getCurrentUser().getRole());
            btnAdmin.setVisible(isAdmin);
            btnAdmin.setManaged(isAdmin);
        }
    }

    @FXML private void onLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onLogout() {
        AuthService.logout();
        refreshNavbar();
    }

    @FXML private void onAdminBackoffice() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/MainView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setTitle("Admin Dashboard");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setTitle("NexUS Gaming Arena");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/RegisterView.fxml"));
            Stage stage = (Stage) flowProduits.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setTitle("Inscription");
        } catch (Exception e) { e.printStackTrace(); }
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

    private void showNotifRecherche(String message, boolean success) {
        Stage notif = new Stage();
        notif.initStyle(StageStyle.TRANSPARENT);
        notif.initModality(Modality.NONE);

        Stage mainStage = (Stage) flowProduits.getScene().getWindow();

        Label lblMsg = new Label(message);
        lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblSub = new Label(success
                ? "Résultats de recherche par image"
                : "Essayez avec une autre image ou vérifiez l'API Imagga");
        lblSub.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11px;");

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: transparent;");
        btnClose.setOnAction(e -> notif.close());

        VBox texts = new VBox(4, lblMsg, lblSub);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox root = new HBox(12, texts, btnClose);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setPrefWidth(400);
        root.setStyle(
                "-fx-background-color: #1a0a3e;" +
                        "-fx-border-color: " + (success ? "#a855f7" : "#f87171") + ";" +
                        "-fx-border-width: 1.5px; -fx-border-radius: 14px;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.7), 24, 0.4, 0, 4);"
        );

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        notif.setScene(scene);

        notif.setX(mainStage.getX() + mainStage.getWidth() - 430);
        notif.setY(mainStage.getY() + mainStage.getHeight() - 110);
        notif.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> notif.close());
        pause.play();
    }
}