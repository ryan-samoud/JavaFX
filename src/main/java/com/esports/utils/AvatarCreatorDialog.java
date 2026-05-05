package com.esports.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AvatarCreatorDialog {

    private final Stage  stage;
    private final int    userId;
    private final Stage  owner;

    private BufferedImage sourcePhoto   = null;
    private BufferedImage cartoonResult = null;
    private int           variation     = 0;

    private final ImageView previewOriginal;
    private final ImageView previewCartoon;
    private final Label     lblHint;
    private final Button    btnRegen;
    private final Button    btnUse;

    private String savedPath = null;

    public AvatarCreatorDialog(Stage owner, int userId) {
        this.owner  = owner;
        this.userId = userId;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(owner);
        stage.setResizable(false);

        // ── Header ──────────────────────────────────────────────────
        Label ico   = new Label("🎨");
        ico.setStyle("-fx-font-size: 20px;");
        Label title = new Label("Créer mon avatar");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-cursor: hand;");
        btnX.setOnAction(e -> stage.close());

        HBox header = new HBox(10, ico, title, sp, btnX);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 22, 16, 22));
        header.setStyle("-fx-background-color: linear-gradient(to right,#3b1f6e,#6b21a8);");

        // ── Previews ────────────────────────────────────────────────
        previewOriginal = new ImageView();
        previewOriginal.setFitWidth(180);
        previewOriginal.setFitHeight(180);
        previewOriginal.setPreserveRatio(true);
        previewOriginal.setStyle(
            "-fx-border-color: rgba(139,92,246,0.3); -fx-border-width: 2px;");

        previewCartoon = new ImageView();
        previewCartoon.setFitWidth(180);
        previewCartoon.setFitHeight(180);
        previewCartoon.setPreserveRatio(true);
        previewCartoon.setStyle(
            "-fx-border-color: rgba(168,85,247,0.6); -fx-border-width: 2px;");

        Label lOrig = new Label("Photo originale");
        lOrig.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        Label lCart = new Label("Aperçu avatar");
        lCart.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox colLeft  = new VBox(8, lOrig, previewOriginal);
        colLeft.setAlignment(Pos.TOP_CENTER);
        VBox colRight = new VBox(8, lCart, previewCartoon);
        colRight.setAlignment(Pos.TOP_CENTER);

        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 28px; -fx-font-weight: bold;");

        HBox previews = new HBox(18, colLeft, arrow, colRight);
        previews.setAlignment(Pos.CENTER);
        previews.setPadding(new Insets(22, 28, 12, 28));
        previews.setStyle("-fx-background-color: #0d0b1e;");

        // ── Hint ─────────────────────────────────────────────────────
        lblHint = new Label("Choisissez une photo pour commencer.");
        lblHint.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        lblHint.setWrapText(true);
        HBox hintBox = new HBox(lblHint);
        hintBox.setPadding(new Insets(0, 28, 8, 28));
        hintBox.setStyle("-fx-background-color: #0d0b1e;");

        // ── Action buttons ───────────────────────────────────────────
        Button btnFile = new Button("📁  Choisir une photo");
        style(btnFile, "rgba(99,102,241,0.2)", "#818cf8", "rgba(99,102,241,0.5)");
        btnFile.setOnAction(e -> pickFile());

        Button btnCam = new Button("📷  Webcam");
        style(btnCam, "rgba(99,102,241,0.15)", "#818cf8", "rgba(99,102,241,0.4)");
        btnCam.setOnAction(e -> pickWebcam());

        btnRegen = new Button("🔄  Régénérer");
        style(btnRegen, "rgba(124,58,237,0.2)", "#a855f7", "rgba(168,85,247,0.5)");
        btnRegen.setDisable(true);
        btnRegen.setOnAction(e -> regenerate());

        btnUse = new Button("✓  Utiliser cet avatar");
        btnUse.setStyle(
            "-fx-background-color: linear-gradient(to right,#7c3aed,#a855f7);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-padding: 10 24 10 24; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnUse.setDisable(true);
        btnUse.setOnAction(e -> saveAndClose());

        HBox actions = new HBox(10, btnFile, btnCam, btnRegen, btnUse);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 28, 22, 28));
        actions.setStyle("-fx-background-color: #0d0b1e;");

        // ── Root ─────────────────────────────────────────────────────
        VBox root = new VBox(header, previews, hintBox, actions);
        root.setStyle(
            "-fx-background-color: #0d0b1e;" +
            "-fx-border-color: rgba(139,92,246,0.45);" +
            "-fx-border-width: 1.5; -fx-border-radius: 14; -fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian,rgba(139,92,246,0.4),30,0.3,0,0);");

        Scene scene = new Scene(root, 520, 380);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
    }

    /** Opens the dialog and returns the saved avatar path, or null if cancelled. */
    public String show() {
        stage.showAndWait();
        return savedPath;
    }

    // ─────────────────────────────────────────────────────────────────
    private void pickFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.bmp","*.gif"));
        File f = fc.showOpenDialog(owner);
        if (f == null) return;
        try {
            sourcePhoto = ImageIO.read(f);
            if (sourcePhoto == null) {
                setHint("Impossible de lire cette image.", "#f87171");
                return;
            }
            showOriginal(sourcePhoto);
            variation = 0;
            applyCartoon();
        } catch (IOException ex) {
            setHint("Erreur : " + ex.getMessage(), "#f87171");
        }
    }

    private void pickWebcam() {
        FaceIdCaptureDialog cam = new FaceIdCaptureDialog(owner);
        BufferedImage captured = cam.show();
        if (captured == null) {
            setHint("Aucune image capturée.", "#fb923c");
            return;
        }
        sourcePhoto = captured;
        showOriginal(sourcePhoto);
        variation = 0;
        applyCartoon();
    }

    private void regenerate() {
        if (sourcePhoto == null) return;
        variation = (variation + 1) % 10;
        applyCartoon();
        setHint("Style " + (variation + 1) + "/10 — cliquez encore pour un autre style.", "#a855f7");
    }

    private void applyCartoon() {
        cartoonResult = CartoonAvatarGenerator.cartoonify(sourcePhoto, variation);
        WritableImage fx = SwingFXUtils.toFXImage(cartoonResult, null);
        previewCartoon.setImage(fx);
        btnRegen.setDisable(false);
        btnUse.setDisable(false);
        setHint("✓ Avatar prêt — cliquez sur Régénérer pour changer le style.", "#4ade80");
    }

    private void showOriginal(BufferedImage img) {
        WritableImage fx = SwingFXUtils.toFXImage(img, null);
        previewOriginal.setImage(fx);
    }

    private void saveAndClose() {
        if (cartoonResult == null) return;
        try {
            String dir  = System.getProperty("user.home") + "/.nexus_avatars";
            Files.createDirectories(Paths.get(dir));
            String path = dir + "/" + userId + ".png";
            ImageIO.write(cartoonResult, "png", new File(path));
            savedPath = path;
            stage.close();
        } catch (IOException e) {
            setHint("Erreur lors de la sauvegarde : " + e.getMessage(), "#f87171");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private void setHint(String msg, String color) {
        lblHint.setText(msg);
        lblHint.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }

    private void style(Button btn, String bg, String text, String border) {
        btn.setStyle(
            "-fx-background-color: " + bg + "; -fx-text-fill: " + text + ";" +
            "-fx-border-color: " + border + "; -fx-border-radius: 8px;" +
            "-fx-background-radius: 8px; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: 9 16 9 16; -fx-cursor: hand;");
    }
}