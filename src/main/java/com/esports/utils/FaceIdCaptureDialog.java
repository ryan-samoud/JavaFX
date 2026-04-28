package com.esports.utils;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class FaceIdCaptureDialog {

    private final Stage stage;
    private final ImageView cameraView;
    private final Label statusLabel;
    private BufferedImage capturedFace;
    private volatile boolean running = false;
    private volatile boolean shouldCapture = false;

    public FaceIdCaptureDialog(Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Face ID — Reconnaissance faciale");
        stage.setResizable(false);

        cameraView = new ImageView();
        cameraView.setFitWidth(400);
        cameraView.setFitHeight(300);
        cameraView.setPreserveRatio(true);
        cameraView.setStyle("-fx-border-color: rgba(168,85,247,0.4); -fx-border-width: 2px;");

        Label title = new Label("👤  Reconnaissance faciale");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        statusLabel = new Label("Positionnez votre visage dans le cadre...");
        statusLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        Button captureBtn = new Button("📷  Capturer mon visage");
        captureBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 24 10 24; " +
                "-fx-background-radius: 8px; -fx-cursor: hand;");
        captureBtn.setOnAction(e -> {
            statusLabel.setText("Analyse en cours...");
            statusLabel.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 12px;");
            shouldCapture = true;
        });

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                "-fx-border-color: #374151; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                "-fx-padding: 10 24 10 24; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            running = false;
            stage.close();
        });

        HBox buttons = new HBox(12, captureBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(14, title, cameraView, statusLabel, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0d0b1e;");

        stage.setScene(new Scene(root, 460, 430));
        stage.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setOnCloseRequest(e -> running = false);
    }

    public BufferedImage show() {
        startGrabber();
        stage.showAndWait();
        return capturedFace;
    }

    private void startGrabber() {
        running = true;
        Thread thread = new Thread(() -> {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> statusLabel.setText("Aucune caméra détectée."));
                return;
            }
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
            try {
                while (running) {
                    BufferedImage frame = webcam.getImage();
                    if (frame == null) { Thread.sleep(33); continue; }

                    WritableImage fxImage = bufferedToFxImage(frame);
                    Platform.runLater(() -> cameraView.setImage(fxImage));

                    if (shouldCapture) {
                        shouldCapture = false;
                        BufferedImage face = FaceIdService.detectAndCropFace(frame);
                        if (face != null) {
                            capturedFace = face;
                            Platform.runLater(() -> {
                                statusLabel.setText("✓ Visage capturé avec succès !");
                                statusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");
                            });
                            Thread.sleep(900);
                            running = false;
                            Platform.runLater(stage::close);
                        } else {
                            Platform.runLater(() -> {
                                statusLabel.setText("Aucun visage détecté. Réessayez.");
                                statusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                            });
                        }
                    }
                    Thread.sleep(33);
                }
            } catch (Exception e) {
                System.err.println("[Camera] " + e.getMessage());
            } finally {
                try { webcam.close(); } catch (Exception ignored) {}
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private WritableImage bufferedToFxImage(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        WritableImage fxImg = new WritableImage(w, h);
        PixelWriter pw = fxImg.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, img.getRGB(x, y));
            }
        }
        return fxImg;
    }
}