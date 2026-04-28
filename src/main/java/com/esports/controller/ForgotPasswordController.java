package com.esports.controller;

import com.esports.model.User;
import com.esports.service.UserService;
import com.esports.utils.EmailService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private Button   btnBack;
    @FXML private VBox     step1Panel;
    @FXML private VBox     step2Panel;

    // Step 1
    @FXML private TextField fieldEmail;
    @FXML private Label     lblStep1Error;
    @FXML private Label     lblStep1Info;
    @FXML private Button    btnSend;

    // Step 2
    @FXML private TextField fieldCode;
    @FXML private Label     lblCodeSentTo;
    @FXML private Label     lblStep2Error;
    @FXML private Button    btnVerify;

    private final UserService    userService = new UserService();
    private final SecureRandom   random      = new SecureRandom();

    /** Email currently being reset — kept across both steps. */
    private String pendingEmail;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblStep1Error.setText("");
        lblStep1Info.setText("");
        lblStep2Error.setText("");
        // Step 1 visible by default, step 2 hidden
        showStep(1);
    }

    // ─────────────────────────────────────────────────────
    // STEP 1 — send code
    // ─────────────────────────────────────────────────────
    @FXML
    private void onSendCode() {
        lblStep1Error.setText("");
        lblStep1Info.setText("");

        String email = fieldEmail.getText().trim();
        if (email.isBlank()) {
            lblStep1Error.setText("⚠ Veuillez entrer votre adresse e-mail.");
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            lblStep1Error.setText("⚠ Format d'e-mail invalide.");
            return;
        }

        pendingEmail = email;
        btnSend.setDisable(true);
        btnSend.setText("Envoi en cours...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Check user exists (any status — suspended users can still reset)
                Optional<User> opt = userService.findByEmailAny(email);
                if (opt.isEmpty()) {
                    // Return neutral message to avoid email enumeration
                    return "NOT_FOUND";
                }
                User user = opt.get();

                // Generate 6-digit code
                String code = String.format("%06d", random.nextInt(1_000_000));
                LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

                // Persist token — throw if it fails so the Task reports the error
                boolean saved = userService.saveResetToken(email, code, expiry);
                if (!saved) {
                    throw new RuntimeException("Impossible de sauvegarder le code en base de données.");
                }

                // Send email
                EmailService.sendPasswordResetCode(email, user.getNom(), code);
                return "OK";
            }
        };

        task.setOnSucceeded(e -> {
            btnSend.setDisable(false);
            btnSend.setText("ENVOYER LE CODE");
            String result = task.getValue();
            if ("NOT_FOUND".equals(result)) {
                // Don't reveal whether email exists — show the same success UI
                // to prevent email enumeration attacks
                lblStep1Info.setText("✔ Si cet e-mail existe, un code a été envoyé.");
            } else {
                lblCodeSentTo.setText("Un code à 6 chiffres a été envoyé à : " + email);
                showStep(2);
            }
        });

        task.setOnFailed(e -> {
            btnSend.setDisable(false);
            btnSend.setText("ENVOYER LE CODE");
            Throwable ex = task.getException();
            System.err.println("[ForgotPassword] send failed: " + ex.getMessage());
            lblStep1Error.setText("✗ Impossible d'envoyer l'e-mail. Vérifiez la configuration SMTP.");
        });

        new Thread(task).start();
    }

    // ─────────────────────────────────────────────────────
    // STEP 2 — verify code
    // ─────────────────────────────────────────────────────
    @FXML
    private void onVerifyCode() {
        lblStep2Error.setText("");
        String code = fieldCode.getText().trim();

        if (code.isBlank()) {
            lblStep2Error.setText("⚠ Veuillez entrer le code reçu par e-mail.");
            return;
        }
        if (!code.matches("\\d{6}")) {
            lblStep2Error.setText("⚠ Le code doit contenir 6 chiffres.");
            return;
        }

        btnVerify.setDisable(true);
        btnVerify.setText("Vérification...");

        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() {
                return userService.findByResetToken(code);
            }
        };

        task.setOnSucceeded(e -> {
            btnVerify.setDisable(false);
            btnVerify.setText("VÉRIFIER LE CODE");
            Optional<User> opt = task.getValue();

            if (opt.isEmpty()) {
                lblStep2Error.setText("✗ Code invalide ou expiré. Veuillez redemander un code.");
            } else {
                User user = opt.get();
                // Hand off the userId to the reset controller, then navigate
                ResetPasswordController.pendingResetUserId = user.getId();
                navigateTo("/com/esports/fxml/ResetPasswordView.fxml", "Nouveau mot de passe");
            }
        });

        task.setOnFailed(e -> {
            btnVerify.setDisable(false);
            btnVerify.setText("VÉRIFIER LE CODE");
            lblStep2Error.setText("✗ Erreur serveur. Réessayez.");
        });

        new Thread(task).start();
    }

    // ─────────────────────────────────────────────────────
    // RESEND CODE — go back to step 1 pre-filled
    // ─────────────────────────────────────────────────────
    @FXML
    private void onResendCode() {
        lblStep2Error.setText("");
        fieldCode.clear();
        showStep(1);
        if (pendingEmail != null) fieldEmail.setText(pendingEmail);
    }

    // ─────────────────────────────────────────────────────
    // BACK → Login
    // ─────────────────────────────────────────────────────
    @FXML
    private void onBack() {
        navigateTo("/com/esports/fxml/LoginView.fxml", "Connexion");
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────
    private void showStep(int step) {
        boolean s1 = (step == 1);
        step1Panel.setVisible(s1);
        step1Panel.setManaged(s1);
        step2Panel.setVisible(!s1);
        step2Panel.setManaged(!s1);
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnBack.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}