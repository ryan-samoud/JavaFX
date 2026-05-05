package com.esports.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class GamertagGeneratorDialog {

    private final Stage        stage;
    private final TextField    fieldGame;
    private final ComboBox<String> cmbStyle;
    private final TextField    fieldKeyword;
    private final FlowPane     resultPane;
    private final Label        lblStatus;
    private final Button       btnGenerate;

    public GamertagGeneratorDialog(Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);

        // ── Header ──────────────────────────────────────────────────────
        Label ico   = new Label("🎮");
        ico.setStyle("-fx-font-size: 22px;");
        Label title = new Label("Générateur de Gamertag");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af;" +
                          "-fx-cursor: hand; -fx-font-size: 14px;");
        btnClose.setOnAction(e -> stage.close());

        HBox header = new HBox(10, ico, title, spacer, btnClose);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right,#3b1f6e,#6b21a8);");

        // ── Form ────────────────────────────────────────────────────────
        fieldGame = new TextField();
        fieldGame.setPromptText("Valorant, LoL, Fortnite, Minecraft, CSGO, Apex...");
        styleInput(fieldGame);

        cmbStyle = new ComboBox<>();
        cmbStyle.getItems().addAll(
                "Libre", "Agressif 🔥", "Mystérieux 🌑",
                "Légendaire ⚡", "Drôle 😂", "Technique 💻");
        cmbStyle.setValue("Libre");
        cmbStyle.setMaxWidth(Double.MAX_VALUE);
        cmbStyle.setStyle(
                "-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: white;" +
                "-fx-border-color: rgba(139,92,246,0.4);" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;");

        fieldKeyword = new TextField();
        fieldKeyword.setPromptText("Shadow, Fire, Neo... (optionnel)");
        styleInput(fieldKeyword);

        VBox form = new VBox(14,
                row("🎮  Jeu favori", fieldGame),
                row("🎭  Style", cmbStyle),
                row("💡  Mot clé", fieldKeyword));
        form.setPadding(new Insets(22, 32, 12, 32));
        form.setStyle("-fx-background-color: #0d0b1e;");

        // ── Results ─────────────────────────────────────────────────────
        resultPane = new FlowPane(8, 8);
        resultPane.setPadding(new Insets(4, 32, 4, 32));
        resultPane.setPrefWrapLength(420);
        resultPane.setStyle("-fx-background-color: #0d0b1e;");

        lblStatus = new Label("Remplissez les champs et cliquez sur Générer.");
        lblStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        lblStatus.setWrapText(true);
        HBox statusBox = new HBox(lblStatus);
        statusBox.setPadding(new Insets(2, 32, 2, 32));
        statusBox.setStyle("-fx-background-color: #0d0b1e;");

        // ── Footer ──────────────────────────────────────────────────────
        btnGenerate = new Button("✨  Générer des gamertags");
        btnGenerate.setStyle(
                "-fx-background-color: linear-gradient(to right,#7c3aed,#a855f7);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;" +
                "-fx-padding: 12 32 12 32; -fx-background-radius: 10px; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian,rgba(168,85,247,0.5),12,0.3,0,0);");
        btnGenerate.setOnAction(e -> onGenerate());
        // Enter key triggers generate from any field
        fieldGame.setOnAction(e -> onGenerate());
        fieldKeyword.setOnAction(e -> onGenerate());

        HBox footer = new HBox(btnGenerate);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 32, 24, 32));
        footer.setStyle("-fx-background-color: #0d0b1e;");

        // ── Root ────────────────────────────────────────────────────────
        VBox root = new VBox(header, form, resultPane, statusBox, footer);
        root.setStyle(
                "-fx-background-color: #0d0b1e;" +
                "-fx-border-color: rgba(139,92,246,0.45);" +
                "-fx-border-width: 1.5; -fx-border-radius: 14; -fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(139,92,246,0.4),30,0.3,0,0);");

        Scene scene = new Scene(root, 490, 430);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────
    private void onGenerate() {
        String game    = fieldGame.getText().trim();
        String style   = cmbStyle.getValue();
        String keyword = fieldKeyword.getText().trim();

        List<String> tags = GamertagAiService.generate(game, style, keyword);

        resultPane.getChildren().clear();
        for (String tag : tags) resultPane.getChildren().add(buildChip(tag));

        setStatus("✓ " + tags.size() + " gamertags générés — cliquez pour copier.", "#4ade80");
        btnGenerate.setText("🔄  Générer d'autres");
        stage.sizeToScene();
    }

    // ─────────────────────────────────────────────────────────────────────
    private Button buildChip(String tag) {
        Button chip = new Button(tag);
        chip.setStyle(chipStyle("#c4b5fd", "rgba(124,58,237,0.2)", "rgba(168,85,247,0.5)"));
        chip.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(tag);
            Clipboard.getSystemClipboard().setContent(cc);
            chip.setText("✓ " + tag);
            chip.setStyle(chipStyle("#4ade80", "rgba(74,222,128,0.15)", "rgba(74,222,128,0.5)"));
        });
        return chip;
    }

    private String chipStyle(String text, String bg, String border) {
        return "-fx-background-color: " + bg + ";" +
               "-fx-text-fill: " + text + ";" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1.5; -fx-border-radius: 20; -fx-background-radius: 20;" +
               "-fx-font-size: 13px; -fx-font-weight: bold;" +
               "-fx-padding: 8 18 8 18; -fx-cursor: hand;";
    }

    private VBox row(String labelText, Control input) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px; -fx-font-weight: bold;");
        return new VBox(5, lbl, input);
    }

    private void styleInput(TextField tf) {
        tf.setStyle(
                "-fx-background-color: rgba(139,92,246,0.1);" +
                "-fx-text-fill: white; -fx-prompt-text-fill: #4b5563;" +
                "-fx-border-color: rgba(139,92,246,0.35);" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                "-fx-padding: 9 14 9 14; -fx-font-size: 13px;");
    }

    private void setStatus(String msg, String color) {
        lblStatus.setText(msg);
        lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }
}