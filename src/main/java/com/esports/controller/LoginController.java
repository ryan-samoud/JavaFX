package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import javafx.animation.PauseTransition;
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

            if (result.isSuccess()) {
                lblSuccess.setText("✔ Connexion réussie ! Redirection...");
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(ev -> goHome());
                pause.play();
            } else {
                lblError.setText("✗ " + result.getMessage());
                fieldPassword.clear();
            }
        });

        task.setOnFailed(e -> {
            btnLogin.setDisable(false);
            btnLogin.setText("SE CONNECTER");
            lblError.setText("✗ Erreur serveur.");
        });

        new Thread(task).start();
    }

    @FXML
    private void onBack() {
        goHome();
    }

    @FXML
    private void onGoRegister() {
        navigateTo("/com/esports/fxml/RegisterView.fxml", "Inscription");
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