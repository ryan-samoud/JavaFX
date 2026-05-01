package com.esports.controller;

import com.esports.service.UserService;
import com.esports.utils.PasswordUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {

    /**
     * Set by ForgotPasswordController immediately before navigating here,
     * so this screen knows which user to update.
     */
    public static int pendingResetUserId = -1;

    @FXML private PasswordField fieldNewPassword;
    @FXML private PasswordField fieldConfirmPassword;
    @FXML private Label         lblError;
    @FXML private Label         lblSuccess;
    @FXML private Button        btnReset;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblError.setText("");
        lblSuccess.setText("");

        // If no userId was set, something went wrong — go back to login.
        // Use Platform.runLater so the scene is fully attached before navigating.
        if (pendingResetUserId < 0) {
            Platform.runLater(this::navigateToLogin);
            return;
        }

        // Allow submitting with Enter from confirm field
        fieldConfirmPassword.setOnAction(e -> onReset());
    }

    @FXML
    private void onReset() {
        lblError.setText("");
        lblSuccess.setText("");

        String newPwd     = fieldNewPassword.getText();
        String confirmPwd = fieldConfirmPassword.getText();

        if (newPwd.isBlank()) {
            lblError.setText("⚠ Veuillez entrer un nouveau mot de passe.");
            return;
        }
        if (newPwd.length() < 8) {
            lblError.setText("⚠ Le mot de passe doit contenir au moins 8 caractères.");
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            lblError.setText("⚠ Les mots de passe ne correspondent pas.");
            fieldConfirmPassword.clear();
            return;
        }

        final int userId = pendingResetUserId;
        btnReset.setDisable(true);
        btnReset.setText("Mise à jour...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                String hashed = PasswordUtil.hash(newPwd);
                boolean updated = userService.updatePassword(userId, hashed);
                if (updated) userService.clearResetToken(userId);
                return updated;
            }
        };

        task.setOnSucceeded(e -> {
            btnReset.setDisable(false);
            btnReset.setText("RÉINITIALISER LE MOT DE PASSE");
            if (task.getValue()) {
                pendingResetUserId = -1;
                fieldNewPassword.clear();
                fieldConfirmPassword.clear();
                lblSuccess.setText("✔ Mot de passe mis à jour ! Redirection vers la connexion...");
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(ev -> navigateToLogin());
                pause.play();
            } else {
                lblError.setText("✗ Erreur lors de la mise à jour. Réessayez.");
            }
        });

        task.setOnFailed(e -> {
            btnReset.setDisable(false);
            btnReset.setText("RÉINITIALISER LE MOT DE PASSE");
            lblError.setText("✗ Erreur serveur. Réessayez.");
        });

        new Thread(task).start();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnReset.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}