package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import com.esports.service.AuthService.AuthResult;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private TextField fieldUsername;
    @FXML private PasswordField fieldPassword;

    @FXML private Label lblLoginError;
    @FXML private Label lblLoginSuccess;

    @FXML private Label lblConnectedUser;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnConnexion;
    @FXML private Button btnInscrire;
    @FXML private Button btnSubmitLogin;
    @FXML private Button btnAdmin;

    // Flag statique : si true, ne pas rediriger vers le backoffice même si connecté
    // Utilisé quand l'admin clique sur le logo pour revenir au front
    public static boolean skipAutoRedirect = false;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        clearMessages();

        // ── SESSION PERSISTANTE ──────────────────────────────────
        // Si skipAutoRedirect=true, on reste sur le front (retour via logo)
        // On remet le flag à false pour les prochaines fois
        if (skipAutoRedirect) {
            skipAutoRedirect = false;
            // Restaurer visuellement l'état connecté sans rediriger
            if (AuthService.isLoggedIn()) {
                User logged = AuthService.getCurrentUser();
                if (logged.isAdmin()) {
                    showSuccess("✔ Admin connecté : " + logged.getNom() + "  |  cliquez ⚙ pour le backoffice");
                    showLoggedInNavbar(logged.getNom() + " (Admin)", true);
                } else {
                    showSuccess("✔ Bienvenue " + logged.getNom() + " !");
                    showLoggedInNavbar(logged.getNom(), false);
                }
                if (fieldUsername != null) fieldUsername.setText(logged.getEmail());
            }
            fieldPassword.setOnAction(e -> onLogin());
            fieldUsername.textProperty().addListener((o, a, b) -> clearMessages());
            fieldPassword.textProperty().addListener((o, a, b) -> clearMessages());
            return;
        }

        // Si un utilisateur est déjà connecté, restaurer son état
        // sans redemander le login
        if (AuthService.isLoggedIn()) {
            User alreadyLogged = AuthService.getCurrentUser();
            if (alreadyLogged.isAdmin()) {
                showSuccess("✔ Connecté en tant qu'admin : " + alreadyLogged.getNom());
                showLoggedInNavbar(alreadyLogged.getNom() + " (Admin)", true);
            } else {
                showSuccess("✔ Connecté : " + alreadyLogged.getNom());
                showLoggedInNavbar(alreadyLogged.getNom(), false);
            }
            // Pré-remplir le champ username avec l'email
            if (fieldUsername != null) fieldUsername.setText(alreadyLogged.getEmail());
            // Ne pas setup les listeners sur les champs si déjà connecté
            return;
        }
        // ────────────────────────────────────────────────────────

        if (btnAdmin != null) {
            btnAdmin.setVisible(false);
            btnAdmin.setManaged(false);
        }

        fieldPassword.setOnAction(e -> onLogin());

        fieldUsername.textProperty().addListener((o, a, b) -> clearMessages());
        fieldPassword.textProperty().addListener((o, a, b) -> clearMessages());
    }

    // ================= LOGIN =================
    @FXML
    private void onLogin() {

        String email = fieldUsername.getText().trim();
        String password = fieldPassword.getText();

        clearMessages();

        if (email.isEmpty() || password.isEmpty()) {
            showError("⚠ Veuillez remplir tous les champs.");
            return;
        }

        btnSubmitLogin.setDisable(true);
        btnSubmitLogin.setText("Connexion...");

        Task<AuthResult> task = new Task<>() {
            @Override
            protected AuthResult call() {
                return authService.login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            handleLogin(task.getValue());
            btnSubmitLogin.setDisable(false);
            btnSubmitLogin.setText("SE CONNECTER");
        });

        task.setOnFailed(e -> {
            showError("✗ Erreur serveur");
            btnSubmitLogin.setDisable(false);
            btnSubmitLogin.setText("SE CONNECTER");
        });

        new Thread(task).start();
    }

    private void handleLogin(AuthResult result) {

        if (result.isSuccess()) {

            User user = result.getUser();

            if (user.isAdmin()) {
                // Admin → afficher bouton backoffice + message
                showAdminButton();
                showSuccess("✔ Connexion admin réussie — cliquez sur ⚙ pour l'administration");
            } else {
                // Utilisateur → rester sur le front avec session active
                hideAdminButton();
                showSuccess("✔ Bienvenue " + user.getNom() + " !");
                if (fieldUsername != null) fieldUsername.setText(user.getEmail());
                showLoggedInNavbar(user.getNom(), false);
            }

        } else {
            showError("✗ " + result.getMessage());
            fieldPassword.clear();
        }
    }

    // ================= ADMIN =================
    private void showAdminButton() {
        btnAdmin.setVisible(true);
        btnAdmin.setManaged(true);
    }

    private void hideAdminButton() {
        btnAdmin.setVisible(false);
        btnAdmin.setManaged(false);
    }

    @FXML
    private void onAdminBackoffice() {
        navigateTo("/com/esports/fxml/MainView.fxml", "Admin Dashboard");
    }

    // ================= NAVIGATION =================
    @FXML
    private void onRegister() {
        navigateTo("/com/esports/fxml/RegisterView.fxml", "Inscription");
    }

    @FXML
    private void onViewTournaments() {
        navigateTo("/com/esports/fxml/TournamentsPublicView.fxml", "Tournois");
    }

    @FXML
    private void onShop() {
        navigateTo("/com/esports/fxml/ShopView.fxml", "Shop");
    }

    // ================= LOGOUT (front) =================
    @FXML
    private void onLogout() {
        AuthService.logout();
        showLoggedOutNavbar();
        clearMessages();
        if (fieldPassword != null) fieldPassword.clear();
        if (fieldUsername != null) fieldUsername.clear();
        showSuccess("");
    }

    // ================= PLACEHOLDERS (évite crash FXML) =================
    @FXML private void onForgotPassword() { showInfo(); }
    @FXML private void onFollowTournament() { showInfo(); }
    @FXML private void onRegisterTournament() { showInfo(); }
    @FXML private void onRemindTournament() { showInfo(); }
    @FXML private void onGameFPS() { showInfo(); }
    @FXML private void onGameMOBA() { showInfo(); }
    @FXML private void onGameBR() { showInfo(); }
    @FXML private void onGameRTS() { showInfo(); }
    @FXML private void onEvent1() { showInfo(); }
    @FXML private void onEvent2() { showInfo(); }
    @FXML private void onEvent3() { showInfo(); }
    @FXML private void onAddToCart1() { showInfo(); }
    @FXML private void onAddToCart2() { showInfo(); }
    @FXML private void onAddToCart3() { showInfo(); }
    @FXML private void onAddToCart4() { showInfo(); }

    private void showInfo() {
        showSuccess("✔ Action à implémenter");
    }

    // ================= NAVIGATION CORE =================
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) btnSubmitLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);

        } catch (Exception e) {
            e.printStackTrace();
            showError("✗ Erreur navigation : " + e.getMessage());
        }
    }

    // ================= NAVBAR STATE =================

    /** Affiche l'état connecté dans la navbar du front */
    private void showLoggedInNavbar(String name, boolean isAdmin) {
        // Afficher le nom
        if (lblConnectedUser != null) {
            lblConnectedUser.setText("✔ " + name);
            lblConnectedUser.setVisible(true);
            lblConnectedUser.setManaged(true);
        }
        // Afficher bouton Déconnexion
        if (btnDeconnexion != null) {
            btnDeconnexion.setVisible(true);
            btnDeconnexion.setManaged(true);
        }
        // Cacher Connexion + S'inscrire
        if (btnConnexion != null) { btnConnexion.setVisible(false); btnConnexion.setManaged(false); }
        if (btnInscrire  != null) { btnInscrire.setVisible(false);  btnInscrire.setManaged(false);  }
        // Bouton admin
        if (btnAdmin != null) {
            btnAdmin.setVisible(isAdmin);
            btnAdmin.setManaged(isAdmin);
        }
    }

    /** Affiche l'état déconnecté dans la navbar */
    private void showLoggedOutNavbar() {
        if (lblConnectedUser != null) { lblConnectedUser.setVisible(false); lblConnectedUser.setManaged(false); }
        if (btnDeconnexion   != null) { btnDeconnexion.setVisible(false);   btnDeconnexion.setManaged(false);   }
        if (btnConnexion != null) { btnConnexion.setVisible(true); btnConnexion.setManaged(true); }
        if (btnInscrire  != null) { btnInscrire.setVisible(true);  btnInscrire.setManaged(true);  }
        if (btnAdmin != null) { btnAdmin.setVisible(false); btnAdmin.setManaged(false); }
    }

    // ================= NAVBAR USER ================
    private void showNavbarUser(String name) {
        if (lblConnectedUser != null) {
            lblConnectedUser.setText("✔ " + name);
            lblConnectedUser.setVisible(true);
            lblConnectedUser.setManaged(true);
        }
    }

    // ================= MESSAGES =================
    private void showError(String msg) {
        lblLoginError.setText(msg);
        lblLoginSuccess.setText("");
    }

    private void showSuccess(String msg) {
        lblLoginSuccess.setText(msg);
        lblLoginError.setText("");
    }

    private void clearMessages() {
        if (lblLoginError != null) lblLoginError.setText("");
        if (lblLoginSuccess != null) lblLoginSuccess.setText("");
    }
}