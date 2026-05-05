package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import com.esports.utils.FaceIdCaptureDialog;
import com.esports.utils.FaceIdService;
import com.esports.utils.AvatarCreatorDialog;
import com.esports.utils.GamertagGeneratorDialog;
import com.esports.utils.TypingBiometricService;
import com.esports.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
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
    @FXML private Label     lblFaceIdStatus;
    @FXML private Button    btnFaceIdEnroll;
    @FXML private Button    btnFaceIdRemove;
    @FXML private Label     lblTypingStatus;
    @FXML private Button    btnTypingEnable;
    @FXML private Button    btnTypingRedo;
    @FXML private Button    btnTypingRemove;

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

        refreshFaceIdStatus(user);
        refreshTypingStatus(user);
    }

    private void refreshFaceIdStatus(User user) {
        boolean hasface = FaceIdService.hasFaceData(user.getId());
        if (hasface) {
            lblFaceIdStatus.setText("✓ Activé");
            lblFaceIdStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");
            btnFaceIdEnroll.setVisible(false);
            btnFaceIdRemove.setVisible(true);
        } else {
            lblFaceIdStatus.setText("Non configuré");
            lblFaceIdStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
            btnFaceIdEnroll.setVisible(true);
            btnFaceIdRemove.setVisible(false);
        }
    }

    @FXML
    private void onEnrollFace() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Stage owner = (Stage) lblFullName.getScene().getWindow();
        FaceIdCaptureDialog dialog = new FaceIdCaptureDialog(owner);
        BufferedImage face = dialog.show();
        if (face == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun visage détecté. Réessayez en vous plaçant face à la caméra.");
            return;
        }
        String b64 = FaceIdService.imageToBase64(face);
        if (b64 != null && FaceIdService.saveFaceData(user.getId(), b64)) {
            refreshFaceIdStatus(user);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement du Face ID.");
        }
    }

    @FXML
    private void onRemoveFace() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le Face ID ? Vous devrez vous reconnecter avec votre mot de passe.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Supprimer Face ID");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            if (FaceIdService.removeFaceData(user.getId())) {
                refreshFaceIdStatus(user);
            }
        }
    }

    private void refreshTypingStatus(User user) {
        boolean enabled = TypingBiometricService.isEnabled(user.getId());
        boolean hasProfile = TypingBiometricService.hasProfile(user.getId());
        if (enabled) {
            lblTypingStatus.setText(hasProfile ? "✓ Activé — profil enregistré" : "✓ Activé — profil en cours d'apprentissage");
            lblTypingStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");
            btnTypingEnable.setVisible(false);
            btnTypingRedo.setVisible(hasProfile);
            btnTypingRemove.setVisible(true);
        } else {
            lblTypingStatus.setText("Non activé");
            lblTypingStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
            btnTypingEnable.setVisible(true);
            btnTypingRedo.setVisible(false);
            btnTypingRemove.setVisible(false);
        }
    }

    @FXML
    private void onEnableTyping() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Alert info = new Alert(Alert.AlertType.INFORMATION,
                "La protection par rythme de frappe va être activée.\n\n" +
                "Votre profil de frappe sera enregistré automatiquement lors de votre prochain mot de passe saisi. " +
                "Les connexions ultérieures vérifieront votre rythme pour protéger votre compte.",
                ButtonType.OK);
        info.setTitle("Rythme de frappe IA");
        info.setHeaderText("Activer la protection");
        info.showAndWait();
        TypingBiometricService.setEnabled(user.getId(), true);
        refreshTypingStatus(user);
    }

    @FXML
    private void onRedoTyping() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Réinitialiser le profil de frappe ? Le nouveau profil sera appris lors de votre prochaine connexion.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Réentraîner le rythme de frappe");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            TypingBiometricService.resetProfile(user.getId());
            refreshTypingStatus(user);
        }
    }

    @FXML
    private void onDisableTyping() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Désactiver la protection par rythme de frappe ? Votre profil de frappe sera supprimé.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Désactiver Rythme de frappe");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            TypingBiometricService.clearProfile(user.getId());
            refreshTypingStatus(user);
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void onGenerateAvatar() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;
        Stage owner = (Stage) lblFullName.getScene().getWindow();
        AvatarCreatorDialog dialog = new AvatarCreatorDialog(owner, user.getId());
        String path = dialog.show();
        if (path == null) return;

        user.setPhoto(path);
        new UserService().update(user);

        try {
            Image img = new Image(new File(path).toURI().toString(), 90, 90, false, true);
            imgPhoto.setImage(img);
            imgPhoto.setVisible(true);
            lblAvatarBig.setVisible(false);
        } catch (Exception ignored) {}
    }

    @FXML
    private void onGenerateGamertag() {
        Stage owner = (Stage) lblFullName.getScene().getWindow();
        new GamertagGeneratorDialog(owner).show();
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