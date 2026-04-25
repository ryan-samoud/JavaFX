package com.esports.controller;

import com.esports.service.AuthService;
import com.esports.utils.CaptchaDialog;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private Label         lblError;
    @FXML private Label         lblSuccess;
    @FXML private Button        btnLogin;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblError.setText("");
        lblSuccess.setText("");
        fieldPassword.setOnAction(e -> onLogin());
    }

    @FXML
    private void onLogin() {
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText();

        lblError.setText("");
        lblSuccess.setText("");

        if (email.isEmpty() || password.isEmpty()) {
            lblError.setText("⚠ Veuillez remplir tous les champs.");
            return;
        }

        // CAPTCHA verification before login
        if (!CaptchaDialog.show(btnLogin.getScene().getWindow())) {
            lblError.setText("⚠ Vérification de sécurité requise.");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Connexion...");

        Task<AuthService.AuthResult> task = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                return authService.login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            btnLogin.setDisable(false);
            btnLogin.setText("SE CONNECTER");
            AuthService.AuthResult result = task.getValue();

            switch (result.getStatus()) {
                case SUCCESS -> {
                    lblSuccess.setText("✔ Connexion réussie ! Redirection...");
                    PauseTransition pause = new PauseTransition(Duration.seconds(1));
                    pause.setOnFinished(ev -> goHome());
                    pause.play();
                }
                case BANNED -> {
                    fieldPassword.clear();
                    showBannedDialog(result.getBanReason());
                }
                case SUSPENDED -> {
                    fieldPassword.clear();
                    showSuspendedDialog(result.getBanReason(), result.getSuspendedUntil());
                }
                default -> {
                    lblError.setText("✗ " + result.getMessage());
                    fieldPassword.clear();
                }
            }
        });

        task.setOnFailed(e -> {
            btnLogin.setDisable(false);
            btnLogin.setText("SE CONNECTER");
            lblError.setText("✗ Erreur serveur.");
        });

        new Thread(task).start();
    }

    // ─────────────────────────────────────────────────────
    // BANNED DIALOG
    // ─────────────────────────────────────────────────────
    private void showBannedDialog(String reason) {
        Stage modal = buildModal();

        // Header
        HBox header = buildHeader(
                "🚫", "COMPTE BANNI",
                "-fx-background-color: linear-gradient(to right, #4a0f0f, #7a1f1f);");

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(28, 36, 12, 36));
        body.setStyle("-fx-background-color: #1a0f0f;");

        Label msg = new Label("Votre compte a été définitivement banni de la plateforme NexUS.");
        msg.setStyle("-fx-text-fill: #e2d9f3; -fx-font-size: 14px;");
        msg.setWrapText(true);

        VBox reasonBox = new VBox(6);
        Label lReason = new Label("RAISON DU BANNISSEMENT");
        lReason.setStyle("-fx-text-fill: #7c3030; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lReasonVal = new Label(reason != null && !reason.isBlank() ? reason : "Non spécifiée");
        lReasonVal.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px;");
        lReasonVal.setWrapText(true);
        reasonBox.setStyle("-fx-background-color: rgba(239,68,68,0.08);" +
                           "-fx-border-color: rgba(239,68,68,0.25);" +
                           "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                           "-fx-padding: 14 16 14 16;");
        reasonBox.getChildren().addAll(lReason, lReasonVal);
        body.getChildren().addAll(msg, reasonBox);

        // Footer
        HBox footer = buildFooter(modal, "#f87171", "rgba(239,68,68,0.15)",
                "rgba(239,68,68,0.35)", "-fx-background-color: #1a0f0f;");

        VBox root = assembleModal(header, body, footer,
                "rgba(239,68,68,0.35)", "#1a0f0f");
        showModal(modal, root, 500, 320);
    }

    // ─────────────────────────────────────────────────────
    // SUSPENDED DIALOG (with live countdown)
    // ─────────────────────────────────────────────────────
    private void showSuspendedDialog(String reason, LocalDateTime until) {
        Stage modal = buildModal();

        // Header
        HBox header = buildHeader(
                "⏸", "COMPTE SUSPENDU",
                "-fx-background-color: linear-gradient(to right, #3b2a00, #6b4a00);");

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(28, 36, 12, 36));
        body.setStyle("-fx-background-color: #1a1400;");

        Label msg = new Label("Votre compte est temporairement suspendu.");
        msg.setStyle("-fx-text-fill: #e2d9f3; -fx-font-size: 14px;");

        // Reason box
        VBox reasonBox = new VBox(6);
        Label lReason = new Label("RAISON DE LA SUSPENSION");
        lReason.setStyle("-fx-text-fill: #7c5a00; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lReasonVal = new Label(reason != null && !reason.isBlank() ? reason : "Non spécifiée");
        lReasonVal.setStyle("-fx-text-fill: #fb923c; -fx-font-size: 14px;");
        lReasonVal.setWrapText(true);
        reasonBox.setStyle("-fx-background-color: rgba(249,115,22,0.08);" +
                           "-fx-border-color: rgba(249,115,22,0.25);" +
                           "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                           "-fx-padding: 14 16 14 16;");
        reasonBox.getChildren().addAll(lReason, lReasonVal);

        // Countdown box
        VBox countdownBox = new VBox(8);
        countdownBox.setAlignment(Pos.CENTER);
        countdownBox.setStyle("-fx-background-color: rgba(249,115,22,0.05);" +
                              "-fx-border-color: rgba(249,115,22,0.2);" +
                              "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                              "-fx-padding: 14 16 14 16;");

        Label lCountLabel = new Label("SUSPENSION LEVÉE DANS");
        lCountLabel.setStyle("-fx-text-fill: #7c5a00; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label lCountdown = new Label();
        lCountdown.setStyle("-fx-text-fill: #fb923c; -fx-font-size: 26px; -fx-font-weight: bold;" +
                            "-fx-font-family: 'Courier New';");

        countdownBox.getChildren().addAll(lCountLabel, lCountdown);

        // Live countdown
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(until)) {
                lCountdown.setText("00j : 00h : 00m : 00s");
            } else {
                long total   = ChronoUnit.SECONDS.between(now, until);
                long days    = total / 86400;
                long hours   = (total % 86400) / 3600;
                long minutes = (total % 3600) / 60;
                long seconds = total % 60;
                lCountdown.setText(String.format("%02dj : %02dh : %02dm : %02ds",
                        days, hours, minutes, seconds));
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        modal.setOnHidden(e -> timer.stop());

        // Trigger once immediately so it's not blank on open
        LocalDateTime now = LocalDateTime.now();
        long total   = Math.max(0, ChronoUnit.SECONDS.between(now, until));
        long days    = total / 86400;
        long hours   = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        lCountdown.setText(String.format("%02dj : %02dh : %02dm : %02ds",
                days, hours, minutes, seconds));

        body.getChildren().addAll(msg, reasonBox, countdownBox);

        // Footer
        HBox footer = buildFooter(modal, "#fb923c", "rgba(249,115,22,0.12)",
                "rgba(249,115,22,0.3)", "-fx-background-color: #1a1400;");

        VBox root = assembleModal(header, body, footer,
                "rgba(249,115,22,0.3)", "#1a1400");
        showModal(modal, root, 500, 400);
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────

    private Stage buildModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);
        modal.setResizable(false);
        return modal;
    }

    private HBox buildHeader(String icon, String title, String bgStyle) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle(bgStyle);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 22px;");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;" +
                     "-fx-font-family: 'Courier New';");
        header.getChildren().addAll(ico, lbl);
        return header;
    }

    private HBox buildFooter(Stage modal, String textColor, String bgColor,
                             String borderColor, String rootBg) {
        Button btn = new Button("  Fermer  ");
        btn.setStyle("-fx-background-color: " + bgColor + ";" +
                     "-fx-text-fill: " + textColor + ";" +
                     "-fx-border-color: " + borderColor + ";" +
                     "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                     "-fx-font-size: 13px; -fx-font-weight: bold;" +
                     "-fx-padding: 10 28 10 28; -fx-cursor: hand;");
        btn.setOnAction(e -> modal.close());

        HBox footer = new HBox(btn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 36, 24, 36));
        footer.setStyle(rootBg);
        return footer;
    }

    private VBox assembleModal(HBox header, VBox body, HBox footer,
                               String borderColor, String bgColor) {
        VBox root = new VBox(header, body, footer);
        root.setStyle("-fx-background-color: " + bgColor + ";" +
                      "-fx-border-color: " + borderColor + ";" +
                      "-fx-border-width: 1.5; -fx-border-radius: 14; -fx-background-radius: 14;" +
                      "-fx-effect: dropshadow(gaussian, " + borderColor + ", 40, 0.3, 0, 0);");
        return root;
    }

    private void showModal(Stage modal, VBox root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/com/esports/css/dashboard.css").toExternalForm());
        } catch (Exception ignored) {}
        modal.setScene(scene);
        modal.centerOnScreen();
        modal.showAndWait();
    }

    // ─────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────

    @FXML
    private void onBack() { goHome(); }

    @FXML
    private void onGoRegister() {
        navigateTo("/com/esports/fxml/RegisterView.fxml", "Inscription");
    }

    @FXML
    private void onForgotPassword() {
        navigateTo("/com/esports/fxml/ForgotPasswordView.fxml", "Mot de passe oublié");
    }

    private void goHome() {
        navigateTo("/com/esports/fxml/HomeView.fxml", "NEXUS ESPORTS");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
