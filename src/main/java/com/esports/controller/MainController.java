package com.esports.controller;

import com.esports.service.AuthService;
import com.esports.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * CONTROLLER — MainController.java
 * Layout principal admin (sidebar + content dynamique)
 * Palette NexUS Gaming : violet #a855f7 / magenta #ec4899
 */
public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label lblCurrentUser;

    // Sidebar buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnTournaments;
    @FXML private Button btnTeams;
    @FXML private Button btnShop;
    @FXML private Button btnEvents;
    @FXML private Button btnGames;

    private Button activeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Utilisateur connecté
        User user = AuthService.getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getNom());
        } else {
            lblCurrentUser.setText("Guest");
        }

        // Vue par défaut — chemin corrigé vers DashboardView.fxml
        loadView("/com/esports/fxml/DashboardView.fxml");
        activeButton = btnDashboard;
        setActiveButton(btnDashboard);
    }

    // ═══════════════════════════════════════
    // NAVIGATION SIDEBAR — chemins tous corrigés vers /fxml/
    // ═══════════════════════════════════════

    @FXML
    private void onDashboard() {
        navigate("/com/esports/fxml/DashboardView.fxml", btnDashboard);
    }

    @FXML
    private void onUsers() {
        navigate("/com/esports/fxml/UsersView.fxml", btnUsers);
    }

    @FXML
    private void onTournaments() {
        navigate("/com/esports/fxml/TournamentsView.fxml", btnTournaments);
    }

    @FXML
    private void onTeams() {
        // TeamsView pas encore créée — affiche un message dans la console
        System.out.println("[MainController] TeamsView non disponible.");
        setActiveButton(btnTeams);
    }

    @FXML
    private void onShop() {
        System.out.println("[MainController] ShopView non disponible.");
        setActiveButton(btnShop);
    }

    @FXML
    private void onEvents() {
        setActiveButton(btnEvents);
        // EventsView à implémenter
        System.out.println("[MainController] EventsView non disponible.");
    }

    @FXML
    private void onGames() {
        setActiveButton(btnGames);
        // GamesView à implémenter
        System.out.println("[MainController] GamesView non disponible.");
    }

    // ═══════════════════════════════════════
    // HOME (logo click) — retour front sans déconnexion
    // ═══════════════════════════════════════

    @FXML
    private void onHome() {
        try {
            // Indiquer à HomeController de NE PAS rediriger vers le backoffice
            // même si la session est active (on veut rester sur le front)
            com.esports.controller.HomeController.skipAutoRedirect = true;

            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/HomeView.fxml")
            );
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════

    @FXML
    private void onLogout() {
        AuthService.logout();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/HomeView.fxml")
            );
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════
    // NAVIGATION CORE
    // ═══════════════════════════════════════

    private void navigate(String fxmlPath, Button btn) {
        loadView(fxmlPath);
        setActiveButton(btn);
    }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource(fxmlPath)
            );
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[MainController] Erreur chargement view: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════
    // STYLE ACTIVE BUTTON — palette NexUS violet/magenta
    // ═══════════════════════════════════════

    private void setActiveButton(Button btn) {

        // Reset l'ancien bouton actif
        if (activeButton != null) {
            activeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #7c6fa8;" +
                "-fx-font-size: 13px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 10 16 10 16;" +
                "-fx-alignment: CENTER_LEFT;" +
                "-fx-cursor: hand;"
            );
        }

        // Style actif : violet NexUS Gaming
        btn.setStyle(
            "-fx-background-color: rgba(168,85,247,0.15);" +
            "-fx-text-fill: #c084fc;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: transparent transparent transparent #a855f7;" +
            "-fx-border-width: 0 0 0 3;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 10 16 10 16;" +
            "-fx-alignment: CENTER_LEFT;" +
            "-fx-cursor: hand;"
        );

        activeButton = btn;
    }
}
