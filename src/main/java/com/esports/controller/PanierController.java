package com.esports.controller;

import com.esports.model.PanierItem;
import com.esports.service.AuthService;
import com.esports.service.PanierService;
import com.esports.service.PanierSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import java.util.List;
import java.util.ResourceBundle;

public class PanierController implements Initializable {

    @FXML private Label   lblNombreArticles;
    @FXML private VBox    vboxVide;
    @FXML private HBox    hboxContenu;
    @FXML private VBox    vboxItems;
    @FXML private Label   lblSousTotal;
    @FXML private Label   lblTotal;

    private final PanierService panierService = new PanierService();
    private int panierId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        panierId = PanierSession.getPanierId();
        System.out.println("[PANIER] PanierController panierId=" + panierId);

        if (panierId <= 0) {
            int userId = AuthService.isLoggedIn() ? AuthService.getCurrentUser().getId() : 0;
            var panier = panierService.getOrCreatePanier(userId);
            if (panier != null) {
                panierId = panier.getId();
                PanierSession.setPanierId(panierId);
            }
        }

        System.out.println("[PANIER] PanierController final panierId=" + panierId);
        refreshView();
    }

    private void refreshView() {
        List<PanierItem> items = panierService.getItems(panierId);
        int nbArticles = items.stream().mapToInt(PanierItem::getQuantite).sum();
        double total = panierService.getTotal(panierId);

        lblNombreArticles.setText("(" + nbArticles + " article" + (nbArticles > 1 ? "s" : "") + ")");
        lblSousTotal.setText(String.format("%.0f DT", total));
        lblTotal.setText(String.format("%.0f DT", total));

        boolean vide = items.isEmpty();
        vboxVide.setVisible(vide);
        vboxVide.setManaged(vide);
        hboxContenu.setVisible(!vide);
        hboxContenu.setManaged(!vide);

        vboxItems.getChildren().clear();
        for (PanierItem item : items) {
            vboxItems.getChildren().add(buildItemRow(item));
        }
    }

    private HBox buildItemRow(PanierItem item) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: rgba(20,17,43,0.9);" +
                        "-fx-border-color: rgba(139,92,246,0.22);" +
                        "-fx-border-width: 1.5px; -fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px; -fx-padding: 16;"
        );

        ImageView imgView = new ImageView();
        imgView.setFitWidth(80);
        imgView.setFitHeight(80);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(80, 80);
        clip.setArcWidth(10); clip.setArcHeight(10);
        imgView.setClip(clip);

        StackPane imgContainer = new StackPane(imgView);
        imgContainer.setPrefSize(80, 80);
        imgContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #be185d);" +
                        "-fx-background-radius: 10px;"
        );

        if (item.getImageProduit() != null && !item.getImageProduit().isBlank()) {
            try {
                String url = toImageUrl(item.getImageProduit());
                if (url != null) {
                    Image img = new Image(url, 80, 80, false, true, true);
                    imgView.setImage(img);
                }
            } catch (Exception ignored) {}
        }

        VBox infos = new VBox(6);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label lblNom = new Label(item.getNomProduit());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblPrix = new Label(String.format("%.0f DT / unité", item.getPrixUnitaire()));
        lblPrix.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");

        Label lblTotalLigne = new Label(String.format("Total : %.0f DT", item.getTotalLigne()));
        lblTotalLigne.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 14px; -fx-font-weight: bold;");

        infos.getChildren().addAll(lblNom, lblPrix, lblTotalLigne);

        HBox qtyBox = new HBox(8);
        qtyBox.setAlignment(Pos.CENTER);

        Button btnMoins = new Button("−");
        btnMoins.setStyle(
                "-fx-background-color: rgba(139,92,246,0.2); -fx-text-fill: white;" +
                        "-fx-font-size: 16px; -fx-background-radius: 6px;" +
                        "-fx-min-width: 32px; -fx-min-height: 32px; -fx-cursor: hand;"
        );

        Label lblQty = new Label(String.valueOf(item.getQuantite()));
        lblQty.setStyle(
                "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-min-width: 30px; -fx-alignment: CENTER;"
        );

        Button btnPlus = new Button("+");
        btnPlus.setStyle(
                "-fx-background-color: rgba(139,92,246,0.2); -fx-text-fill: white;" +
                        "-fx-font-size: 16px; -fx-background-radius: 6px;" +
                        "-fx-min-width: 32px; -fx-min-height: 32px; -fx-cursor: hand;"
        );

        btnMoins.setOnAction(e -> {
            panierService.modifierQuantite(item.getId(), item.getQuantite() - 1);
            refreshView();
        });
        btnPlus.setOnAction(e -> {
            panierService.modifierQuantite(item.getId(), item.getQuantite() + 1);
            refreshView();
        });

        qtyBox.getChildren().addAll(btnMoins, lblQty, btnPlus);

        Button btnSuppr = new Button("🗑");
        btnSuppr.setStyle(
                "-fx-background-color: rgba(248,113,113,0.1); -fx-text-fill: #f87171;" +
                        "-fx-border-color: rgba(248,113,113,0.4); -fx-border-width: 1px;" +
                        "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                        "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 6 10 6 10;"
        );
        btnSuppr.setOnAction(e -> {
            panierService.supprimerItem(item.getId());
            refreshView();
        });

        row.getChildren().addAll(imgContainer, infos, qtyBox, btnSuppr);
        return row;
    }

    @FXML
    private void onVider() {
        panierService.viderPanier(panierId);
        refreshView();
    }

    @FXML
    private void onBackShop() {
        navigateTo("/com/esports/fxml/ShopPublicView.fxml", "Boutique");
    }

    private void navigateTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) vboxVide.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toImageUrl(String src) {
        if (src == null || src.isBlank()) return null;
        src = src.trim().replace("\\", "/");
        if (src.startsWith("http") || src.startsWith("file:")) return src;
        if (src.length() > 1 && src.charAt(1) == ':') return "file:///" + src;
        return "file:///" + src;
    }
    @FXML
    private void onCommander() {
        if (panierService.getItems(panierId).isEmpty()) return;

        if (!AuthService.isLoggedIn()) {
            // Notification stylisée
            Stage notif = new Stage();
            notif.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            notif.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Label icon = new Label("🔒");
            icon.setStyle("-fx-font-size: 26px;");

            Label lblMsg = new Label("Vous devez être connecté !");
            lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            Label lblSub = new Label("Connectez-vous pour passer une commande.");
            lblSub.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 11px;");

            Button btnLogin = new Button("Se connecter");
            btnLogin.setStyle(
                    "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
                            "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 8px; -fx-padding: 7 16 7 16; -fx-cursor: hand;"
            );
            btnLogin.setOnAction(e -> {
                notif.close();
                try {
                    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                            getClass().getResource("/com/esports/fxml/LoginView.fxml"));
                    Stage stage = (Stage) vboxVide.getScene().getWindow();
                    double w = stage.getWidth(), h = stage.getHeight();
                    stage.setScene(new javafx.scene.Scene(root, w, h));
                    stage.setTitle("Connexion");
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            Button btnFermer = new Button("✕");
            btnFermer.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                            "-fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: transparent;"
            );
            btnFermer.setOnAction(e -> notif.close());

            javafx.scene.layout.VBox texts = new javafx.scene.layout.VBox(4, lblMsg, lblSub, btnLogin);
            texts.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(texts, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.layout.HBox root = new javafx.scene.layout.HBox(12, icon, texts, btnFermer);
            root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            root.setPadding(new javafx.geometry.Insets(16, 20, 16, 20));
            root.setPrefWidth(360);
            root.setStyle(
                    "-fx-background-color: #1a0a3e;" +
                            "-fx-border-color: #f87171;" +
                            "-fx-border-width: 1.5px;" +
                            "-fx-border-radius: 14px;" +
                            "-fx-background-radius: 14px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(248,113,113,0.6), 20, 0.3, 0, 4);"
            );

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            notif.setScene(scene);

            Stage mainStage = (Stage) vboxVide.getScene().getWindow();
            notif.setX(mainStage.getX() + (mainStage.getWidth() - 360) / 2);
            notif.setY(mainStage.getY() + mainStage.getHeight() - 150);
            notif.show();

            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
            pause.setOnFinished(ev -> notif.close());
            pause.play();
            return;
        }

        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/PaiementView.fxml"));
            Stage stage = (Stage) vboxVide.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new javafx.scene.Scene(root, w, h));
            stage.setTitle("Paiement");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}