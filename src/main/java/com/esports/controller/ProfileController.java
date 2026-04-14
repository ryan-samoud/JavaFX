package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label     lblAvatarBig;
    @FXML private ImageView imgPhoto;
    @FXML private Label     lblFullName;
    @FXML private Label     lblRole;
    @FXML private Label     lblEmail;
    @FXML private Label     lblAge;
    @FXML private Label     lblRoleDetail;
    @FXML private Label     lblDate;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = AuthService.getCurrentUser();
        if (user == null) { onBack(); return; }

        String initials = getInitials(user.getNom(), user.getPrenom());
        lblAvatarBig.setText(initials);
        lblFullName.setText(user.getPrenom() + " " + user.getNom());
        lblRole.setText(user.isAdmin() ? "⚙ Administrateur" : "🎮 Joueur");
        lblEmail.setText(user.getEmail());
        lblAge.setText(user.getAge() + " ans");

        if (lblRoleDetail != null) {
            String r = user.getRole();
            lblRoleDetail.setText("spectateur".equalsIgnoreCase(r) ? "👁 Spectateur" :
                                   user.isAdmin()                  ? "⚙ Admin"       : "🎮 Joueur");
        }

        if (user.getDateCreation() != null) {
            lblDate.setText(user.getDateCreation()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            lblDate.setText("—");
        }

        // Afficher la photo si elle existe
        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            try {
                File f = new File(user.getPhoto());
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), 90, 90, false, true);
                    imgPhoto.setImage(img);
                    imgPhoto.setVisible(true);
                    lblAvatarBig.setVisible(false);
                }
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void onEdit() {
        navigateTo("/com/esports/fxml/EditProfileView.fxml", "Modifier mon profil");
    }

    @FXML
    private void onBack() {
        navigateTo("/com/esports/fxml/HomeView.fxml", "NEXUS ESPORTS");
    }

    @FXML
    private void onLogout() {
        AuthService.logout();
        navigateTo("/com/esports/fxml/HomeView.fxml", "NEXUS ESPORTS");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) lblFullName.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInitials(String nom, String prenom) {
        String n = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        return p + n;
    }
}