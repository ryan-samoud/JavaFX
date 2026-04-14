package com.esports.utils;

import com.esports.model.Produit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialogs custom dans le thème NexUS (fond sombre, dégradé violet/rose).
 * Remplace tous les Alert JavaFX standard du projet.
 */
public class NexusDialog {

    // ══════════════════════════════════════════════════════════════
    // COULEURS & STYLES COMMUNS
    // ══════════════════════════════════════════════════════════════
    private static final String BG_DARK      = "#0d0b1e";
    private static final String BG_CARD      = "#110f28";
    private static final String BORDER_COLOR = "rgba(139,92,246,0.35)";
    private static final String TEXT_WHITE   = "white";
    private static final String TEXT_MUTED   = "#9ca3af";
    private static final String TEXT_PURPLE  = "#c4b5fd";

    private static final String BTN_PRIMARY =
        "-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899);" +
        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
        "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;" +
        "-fx-border-color: transparent;";

    private static final String BTN_CANCEL =
        "-fx-background-color: transparent;" +
        "-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-font-weight: bold;" +
        "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;" +
        "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 1.5px; -fx-border-radius: 8px;";

    private static final String BTN_DANGER =
        "-fx-background-color: linear-gradient(to right, #dc2626, #f87171);" +
        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
        "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;" +
        "-fx-border-color: transparent;";

    // ══════════════════════════════════════════════════════════════
    // DIALOG DE CONFIRMATION (Supprimer, Désactiver…)
    // ══════════════════════════════════════════════════════════════

    /**
     * Affiche un dialog de confirmation stylisé.
     * @return true si l'utilisateur clique OK
     */
    public static boolean showConfirm(String title, String header, String body) {
        AtomicBoolean confirmed = new AtomicBoolean(false);

        Stage stage = buildStage(title);

        // ── Icône avertissement ──
        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size: 32px; -fx-text-fill: #f59e0b;");

        // ── Textes ──
        Label lblHeader = new Label(header);
        lblHeader.setStyle("-fx-text-fill: " + TEXT_WHITE + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        lblHeader.setWrapText(true);
        lblHeader.setMaxWidth(340);

        Label lblBody = new Label(body);
        lblBody.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 13px;");
        lblBody.setWrapText(true);
        lblBody.setMaxWidth(340);

        VBox texts = new VBox(6, lblHeader, lblBody);
        texts.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(16, icon, texts);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 0, 20, 0));

        // Séparateur dégradé
        Region sep = gradientSep();

        // ── Boutons ──
        Button btnOk     = new Button("Confirmer");
        Button btnCancel = new Button("Annuler");
        btnOk.setStyle(BTN_DANGER);
        btnCancel.setStyle(BTN_CANCEL);

        btnOk.setOnAction(e -> { confirmed.set(true); stage.close(); });
        btnCancel.setOnAction(e -> stage.close());

        addHover(btnOk, BTN_DANGER,
            "-fx-background-color: linear-gradient(to right, #b91c1c, #ef4444);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;");

        HBox btnRow = new HBox(12, btnCancel, btnOk);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(20, 0, 0, 0));

        VBox root = buildRoot(topRow, sep, btnRow);
        stage.setScene(buildScene(root, 420, 200));
        stage.showAndWait();

        return confirmed.get();
    }

    // ══════════════════════════════════════════════════════════════
    // DIALOG D'INFO (Voir plus produit)
    // ══════════════════════════════════════════════════════════════

    public static void showProduitDetail(Produit p) {
        Stage stage = buildStage(p.getNom());

        // ── Image du produit ──
        StackPane imgPane = null;
        String src = p.getImage();
        if (src != null && !src.isBlank()) {
            try {
                Image img;
                String imgUrl = toImageUrl(src);
                if (imgUrl != null) {
                    img = new Image(imgUrl, 460, 220, false, true, true);
                } else {
                    img = null;
                }
                if (img == null) { imgPane = null; throw new Exception("no image"); }
                ImageView iv = new ImageView(img);
                iv.setFitWidth(460);
                iv.setFitHeight(220);
                iv.setPreserveRatio(false);
                Rectangle clip = new Rectangle(460, 220);
                clip.setArcWidth(12); clip.setArcHeight(12);
                iv.setClip(clip);
                imgPane = new StackPane(iv);
                imgPane.setPrefSize(460, 220);
                imgPane.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #4c1d95, #be185d);" +
                    "-fx-background-radius: 10px 10px 0 0;"
                );
                // fallback si erreur asynchrone
                img.errorProperty().addListener((obs, was, err) -> {
                    if (err) { iv.setImage(null); }
                });
                if (img.isError()) { iv.setImage(null); }
            } catch (Exception ignored) {}
        }

        // ── Bandeau titre avec dégradé ──
        Label lblTitre = new Label(p.getNom());
        lblTitre.setStyle(
            "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;" +
            "-fx-font-family: 'Arial Black';"
        );

        Label lblPrix = new Label(p.getPrixFormate());
        lblPrix.setStyle(
            "-fx-text-fill: #a855f7; -fx-font-size: 22px; -fx-font-weight: bold;"
        );

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(lblTitre, spacer, lblPrix);

        // Barre décorative
        Region bar = new Region();
        bar.setPrefHeight(3);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #ec4899, transparent);");
        VBox.setMargin(bar, new Insets(8, 0, 16, 0));

        // ── Infos ──
        HBox catRow  = infoRow("📁 Catégorie", p.getNomCategorie() != null ? p.getNomCategorie() : "—");
        HBox stockRow = infoRow(
            p.isDisponible() ? "✅ Stock" : "❌ Stock",
            p.getStockLabel()
        );
        if (!p.isDisponible()) {
            ((Label) stockRow.getChildren().get(1)).setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
        }

        // ── Description ──
        VBox descBox = new VBox(6);
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            Label lblDescTitle = new Label("Description");
            lblDescTitle.setStyle("-fx-text-fill: " + TEXT_PURPLE + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            Label lblDesc = new Label(p.getDescription());
            lblDesc.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 13px;");
            lblDesc.setWrapText(true);
            lblDesc.setMaxWidth(420);

            // Fond description
            VBox descCard = new VBox(4, lblDescTitle, lblDesc);
            descCard.setPadding(new Insets(10, 14, 10, 14));
            descCard.setStyle(
                "-fx-background-color: rgba(139,92,246,0.08);" +
                "-fx-border-color: rgba(139,92,246,0.2);" +
                "-fx-border-width: 1px; -fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
            );
            descBox.getChildren().add(descCard);
        }

        Region sep = gradientSep();

        // ── Bouton fermer ──
        Button btnClose = new Button("Fermer");
        btnClose.setStyle(BTN_PRIMARY);
        btnClose.setOnAction(e -> stage.close());
        addHover(btnClose, BTN_PRIMARY,
            "-fx-background-color: linear-gradient(to right, #6d28d9, #db2777);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;");

        HBox btnRow = new HBox(btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(18, 0, 0, 0));

        VBox content = new VBox(0, headerRow, bar, catRow, stockRow);
        content.setSpacing(10);
        if (!descBox.getChildren().isEmpty()) {
            content.getChildren().add(descBox);
        }

        VBox inner = buildRoot(content, sep, btnRow);

        // Si on a une image, on l'affiche au-dessus du contenu dans un VBox englobant
        VBox root;
        if (imgPane != null) {
            // Ajuster le padding du inner pour ne pas avoir d'espace superflu en haut
            inner.setPadding(new Insets(20, 30, 28, 30));
            root = new VBox(0, imgPane, inner);
            root.setStyle(
                "-fx-background-color: #110f28;" +
                "-fx-border-color: rgba(139,92,246,0.45);" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.5), 30, 0.3, 0, 6);"
            );
            inner.setStyle("-fx-background-color: transparent;");
        } else {
            root = inner;
        }

        stage.setScene(buildScene(root, 480, -1));
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // DIALOG INFO SIMPLE (messages généraux)
    // ══════════════════════════════════════════════════════════════

    public static void showInfo(String title, String message) {
        Stage stage = buildStage(title);

        Label icon = new Label("ℹ");
        icon.setStyle("-fx-font-size: 30px; -fx-text-fill: #818cf8;");

        Label lblMsg = new Label(message);
        lblMsg.setStyle("-fx-text-fill: " + TEXT_WHITE + "; -fx-font-size: 14px;");
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(320);

        HBox topRow = new HBox(14, icon, lblMsg);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Region sep = gradientSep();
        sep.setPadding(new Insets(0, 0, 10, 0));

        Button btnOk = new Button("OK");
        btnOk.setStyle(BTN_PRIMARY);
        btnOk.setOnAction(e -> stage.close());
        addHover(btnOk, BTN_PRIMARY,
            "-fx-background-color: linear-gradient(to right, #6d28d9, #db2777);" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-padding: 9 28 9 28; -fx-cursor: hand;");

        HBox btnRow = new HBox(btnOk);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(18, 0, 0, 0));

        VBox root = buildRoot(topRow, sep, btnRow);
        stage.setScene(buildScene(root, 400, 180));
        stage.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS INTERNES
    // ══════════════════════════════════════════════════════════════

    private static Stage buildStage(String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(title);
        stage.setResizable(false);
        return stage;
    }

    private static Scene buildScene(VBox root, double width, double height) {
        Scene scene;
        if (height > 0) {
            scene = new Scene(root, width, height);
        } else {
            scene = new Scene(root);
            root.setPrefWidth(width);
        }
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return scene;
    }

    private static VBox buildRoot(javafx.scene.Node... children) {
        VBox root = new VBox();
        root.getChildren().addAll(children);
        root.setPadding(new Insets(28, 30, 28, 30));
        root.setStyle(
            "-fx-background-color: #110f28;" +
            "-fx-border-color: rgba(139,92,246,0.45);" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 14px;" +
            "-fx-background-radius: 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.5), 30, 0.3, 0, 6);"
        );
        return root;
    }

    private static Region gradientSep() {
        Region sep = new Region();
        sep.setPrefHeight(1.5);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, rgba(236,72,153,0.5), transparent);");
        VBox.setMargin(sep, new Insets(14, 0, 14, 0));
        return sep;
    }

    private static HBox infoRow(String labelText, String valueText) {
        Label lbl = new Label(labelText + " :");
        lbl.setStyle("-fx-text-fill: " + TEXT_PURPLE + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 110px;");

        Label val = new Label(valueText);
        val.setStyle("-fx-text-fill: " + TEXT_WHITE + "; -fx-font-size: 13px;");

        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static void addHover(Button btn, String normal, String hover) {
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
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
