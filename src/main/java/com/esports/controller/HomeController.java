package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private HBox   hboxUserNav;
    @FXML private Button lblAvatar;
    @FXML private Label  lblConnectedUser;
    @FXML private Button btnDeconnexion;
    @FXML private Button btnConnexion;
    @FXML private Button btnInscrire;
    @FXML private Button btnAdmin;

    public static boolean skipAutoRedirect = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (skipAutoRedirect) {
            skipAutoRedirect = false;
        }

        if (AuthService.isLoggedIn()) {
            User user = AuthService.getCurrentUser();
            if (user.isAdmin()) {
                showLoggedInNavbar(user.getNom() + " (Admin)", true);
            } else {
                showLoggedInNavbar(user.getNom(), false);
            }
        } else {
            showLoggedOutNavbar();
        }
    }

    // ================= CONNEXION → page dédiée =================
    @FXML
    private void onLogin() {
        navigateTo("/com/esports/fxml/LoginView.fxml", "Connexion");
    }

    // ================= PROFILE =================
    @FXML
    private void onProfile() {
        navigateTo("/com/esports/fxml/ProfileView.fxml", "Mon Profil");
    }

    // ================= ADMIN =================
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
    private void onViewGames() {
        navigateTo("/com/esports/fxml/GamesPublicView.fxml", "Jeux");
    }

    @FXML
    private void onViewEvents() { navigateTo("/com/esports/fxml/EvenementsPublicView.fxml", "Événements");}


    @FXML
    private void onShop() {
        navigateTo("/com/esports/fxml/ShopView.fxml", "Shop");
    }

    // ================= LOGOUT =================
    @FXML
    private void onLogout() {
        AuthService.logout();
        showLoggedOutNavbar();
    }

    // ================= HOME =================
    @FXML
    private void onHome() {
        navigateTo("/com/esports/fxml/HomeView.fxml", "NEXUS ESPORTS");
    }

    // ================= PLACEHOLDERS =================
    @FXML private void onForgotPassword()      {}
    @FXML private void onFollowTournament()    {}
    @FXML private void onRegisterTournament()  {}
    @FXML private void onRemindTournament()    {}
    @FXML private void onGameFPS()             {}
    @FXML private void onGameMOBA()            {}
    @FXML private void onGameBR()              {}
    @FXML private void onGameRTS()             {}
    @FXML private void onEvent1()              {}
    @FXML private void onEvent2()              {}
    @FXML private void onEvent3()              {}
    @FXML private void onAddToCart1()          {}
    @FXML private void onAddToCart2()          {}
    @FXML private void onAddToCart3()          {}
    @FXML private void onAddToCart4()          {}

    // ================= NAVIGATION CORE =================
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            // Utiliser le premier noeud disponible dans la scène
            javafx.scene.Node anchor = (hboxUserNav != null && hboxUserNav.getScene() != null)
                    ? hboxUserNav : btnConnexion;
            Stage stage = (Stage) anchor.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= NAVBAR STATE =================
    private void showLoggedInNavbar(String name, boolean isAdmin) {
        if (hboxUserNav != null)    { hboxUserNav.setVisible(true);    hboxUserNav.setManaged(true); }
        if (lblConnectedUser != null) lblConnectedUser.setText("✔ " + name);
        if (lblAvatar != null && AuthService.getCurrentUser() != null) {
            User u = AuthService.getCurrentUser();
            setAvatar(u);
        }
        if (btnDeconnexion != null) { btnDeconnexion.setVisible(true);  btnDeconnexion.setManaged(true); }
        if (btnConnexion   != null) { btnConnexion.setVisible(false);   btnConnexion.setManaged(false); }
        if (btnInscrire    != null) { btnInscrire.setVisible(false);    btnInscrire.setManaged(false); }
        if (btnAdmin       != null) { btnAdmin.setVisible(isAdmin);     btnAdmin.setManaged(isAdmin); }
    }

    private void setAvatar(User u) {
        String photo = u.getPhoto();
        if (photo != null && !photo.isBlank()) {
            try {
                Image img = photo.startsWith("http") || photo.startsWith("file:")
                        ? new Image(photo, 36, 36, false, true, true)
                        : new Image("file:" + photo, 36, 36, false, true, true);

                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(36);
                    iv.setFitHeight(36);
                    iv.setPreserveRatio(false);

                    Circle clip = new Circle(18, 18, 18);
                    iv.setClip(clip);

                    lblAvatar.setGraphic(iv);
                    lblAvatar.setText("");
                    lblAvatar.setStyle(
                            "-fx-background-color: transparent;" +
                            "-fx-background-radius: 50%;" +
                            "-fx-min-width: 36px; -fx-min-height: 36px;" +
                            "-fx-max-width: 36px; -fx-max-height: 36px;" +
                            "-fx-padding: 0; -fx-cursor: hand;");
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: initials
        lblAvatar.setGraphic(null);
        lblAvatar.setText(getInitials(u.getNom(), u.getPrenom()));
        lblAvatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right,#7c3aed,#ec4899);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 50%;" +
                "-fx-min-width: 36px; -fx-min-height: 36px;" +
                "-fx-max-width: 36px; -fx-max-height: 36px; -fx-cursor: hand;");
    }

    private void showLoggedOutNavbar() {
        if (hboxUserNav    != null) { hboxUserNav.setVisible(false);    hboxUserNav.setManaged(false); }
        if (btnDeconnexion != null) { btnDeconnexion.setVisible(false); btnDeconnexion.setManaged(false); }
        if (btnConnexion   != null) { btnConnexion.setVisible(true);    btnConnexion.setManaged(true); }
        if (btnInscrire    != null) { btnInscrire.setVisible(true);     btnInscrire.setManaged(true); }
        if (btnAdmin       != null) { btnAdmin.setVisible(false);       btnAdmin.setManaged(false); }
    }

    private String getInitials(String nom, String prenom) {
        String n = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        return p + n;
    }
}