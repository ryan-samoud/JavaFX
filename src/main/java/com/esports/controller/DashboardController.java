package com.esports.controller;

import com.esports.dao.UserDAO;
import com.esports.service.AuthService;
import com.esports.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ===== SIDEBAR =====
    @FXML private Label lblWelcome;

    // ===== KPI =====
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalTournaments;
    @FXML private Label lblTotalTeams;
    @FXML private Label lblTotalProducts;

    // ===== RECENT USERS =====
    @FXML private VBox recentUsersContainer;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = AuthService.getCurrentUser();
        if (user != null && lblWelcome != null) {
            lblWelcome.setText(user.getNom());
        }
        loadKpis();
        loadRecentUsers();
    }

    // ================= KPIs =================
    private void loadKpis() {
        try {
            int count = userDAO.countActive();
            if (lblTotalUsers != null) lblTotalUsers.setText(String.valueOf(count));
        } catch (Exception e) {
            System.err.println("KPIs error: " + e.getMessage());
        }
    }

    // ============ RECENT USERS =============
    private void loadRecentUsers() {
        try {
            List<User> users = userDAO.findRecent(5);
            if (recentUsersContainer == null) return;
            recentUsersContainer.getChildren().clear();
            for (User u : users) {
                recentUsersContainer.getChildren().add(buildRow(u));
            }
        } catch (Exception e) {
            System.err.println("Recent users error: " + e.getMessage());
        }
    }

    private HBox buildRow(User u) {
        HBox row = new HBox();
        row.setStyle(
            "-fx-padding: 12 24 12 24;" +
            "-fx-border-color: transparent transparent rgba(139,92,246,0.1) transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        Label name = new Label(u.getNom() + " " + u.getPrenom());
        name.setStyle("-fx-text-fill: #e2d9f3; -fx-font-size: 13px; -fx-pref-width: 220px;");

        Label email = new Label(u.getEmail());
        email.setStyle("-fx-text-fill: #7c6fa8; -fx-font-size: 13px; -fx-pref-width: 280px;");

        Label role = new Label(u.getRole());
        boolean isAdmin = "admin".equalsIgnoreCase(u.getRole());
        role.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-padding: 3 12 3 12; -fx-background-radius: 20;" +
            (isAdmin
                ? "-fx-background-color: rgba(168,85,247,0.18); -fx-text-fill: #c084fc;" +
                  "-fx-border-color: rgba(168,85,247,0.35); -fx-border-radius: 20; -fx-border-width: 1;"
                : "-fx-background-color: rgba(56,189,248,0.15); -fx-text-fill: #38bdf8;" +
                  "-fx-border-color: rgba(56,189,248,0.3); -fx-border-radius: 20; -fx-border-width: 1;")
        );

        row.getChildren().addAll(name, email, role);
        row.setOnMouseEntered(e ->
            row.setStyle(row.getStyle() + "-fx-background-color: rgba(168,85,247,0.07);")
        );
        row.setOnMouseExited(e -> row.setStyle(
            "-fx-padding: 12 24 12 24;" +
            "-fx-border-color: transparent transparent rgba(139,92,246,0.1) transparent;" +
            "-fx-border-width: 0 0 1 0;"
        ));
        return row;
    }

    // ===== NAVIGATION — chemins tous corrigés vers /fxml/ =====
    @FXML private void onDashboard()   { /* déjà sur le dashboard */ }
    @FXML private void onNewUser()     { loadSubView("/com/esports/fxml/UsersView.fxml"); }
    @FXML private void onTournaments() { loadSubView("/com/esports/fxml/TournamentsView.fxml"); }
    @FXML private void onTeams()       { System.out.println("[Dashboard] TeamsView non disponible."); }
    @FXML private void onShop()        { System.out.println("[Dashboard] ShopView non disponible."); }
    @FXML private void onEvents()      { System.out.println("[Dashboard] EventsView non disponible."); }
    @FXML private void onGames()       { System.out.println("[Dashboard] GamesView non disponible."); }

    @FXML private void onHome() {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            StackPane contentArea = getContentArea();
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onLogout() {
        AuthService.logout();
        onHome();
    }

    // ===== HELPER =====
    private void loadSubView(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxml));
            StackPane contentArea = getContentArea();
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("[DashboardController] Vue introuvable : " + fxml);
            e.printStackTrace();
        }
    }

    private StackPane getContentArea() {
        if (recentUsersContainer != null && recentUsersContainer.getScene() != null) {
            return (StackPane) recentUsersContainer.getScene().lookup("#contentArea");
        }
        return null;
    }
}
