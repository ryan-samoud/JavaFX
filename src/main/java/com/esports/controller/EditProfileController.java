package com.esports.controller;

import com.esports.model.User;
import com.esports.service.AuthService;
import com.esports.service.UserService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.Optional;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class EditProfileController implements Initializable {

    @FXML private Label       lblAvatarEdit;
    @FXML private ImageView   imgPhoto;
    @FXML private Label       lblPhotoPath;

    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldEmail;
    @FXML private TextField     fieldAge;
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldConfirm;

    @FXML private Label lblSuccess;
    @FXML private Label lblError;
    @FXML private Label errNom;
    @FXML private Label errPrenom;
    @FXML private Label errEmail;
    @FXML private Label errAge;
    @FXML private Label errPassword;
    @FXML private Label errConfirm;

    @FXML private Button btnSave;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final UserService userService = new UserService();
    private String selectedPhotoPath = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearErrors();
        User user = AuthService.getCurrentUser();
        if (user == null) { onBack(); return; }

        // Pré-remplir les champs
        fieldNom.setText(user.getNom());
        fieldPrenom.setText(user.getPrenom());
        fieldEmail.setText(user.getEmail());
        fieldAge.setText(String.valueOf(user.getAge()));

        // Afficher photo existante ou initiales
        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            showPhoto(user.getPhoto());
        } else {
            lblAvatarEdit.setText(getInitials(user.getNom(), user.getPrenom()));
        }
    }

    // ── Choisir une photo ────────────────────────────────────────────
    @FXML
    private void onPickPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = chooser.showOpenDialog(btnSave.getScene().getWindow());
        if (file != null) {
            selectedPhotoPath = file.getAbsolutePath();
            lblPhotoPath.setText(file.getName());
            showPhoto(selectedPhotoPath);
        }
    }

    // ── Sauvegarder ─────────────────────────────────────────────────
    @FXML
    private void onSave() {
        clearErrors();

        User user = AuthService.getCurrentUser();
        if (user == null) return;

        // Confirmation dialog before saving
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer les modifications");
        confirmAlert.setHeaderText("Enregistrer les modifications ?");
        confirmAlert.setContentText("Voulez-vous vraiment mettre à jour votre profil ?");
        Optional<ButtonType> alertResult = confirmAlert.showAndWait();
        if (alertResult.isEmpty() || alertResult.get() != ButtonType.OK) return;

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String ageStr   = fieldAge.getText().trim();
        String password = fieldPassword.getText();
        String confirm  = fieldConfirm.getText();

        boolean valid = true;

        if (nom.isEmpty() || nom.length() < 2) {
            errNom.setText(nom.isEmpty() ? "Obligatoire." : "Minimum 2 caractères.");
            valid = false;
        }
        if (prenom.isEmpty() || prenom.length() < 2) {
            errPrenom.setText(prenom.isEmpty() ? "Obligatoire." : "Minimum 2 caractères.");
            valid = false;
        }
        if (email.isEmpty()) {
            errEmail.setText("Obligatoire.");
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errEmail.setText("Format email invalide.");
            valid = false;
        }

        int age = 0;
        if (ageStr.isEmpty()) {
            errAge.setText("Obligatoire.");
            valid = false;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age < 13 || age > 120) { errAge.setText("Âge invalide (13-120)."); valid = false; }
            } catch (NumberFormatException e) {
                errAge.setText("Doit être un nombre.");
                valid = false;
            }
        }

        // Mot de passe — optionnel
        if (!password.isEmpty()) {
            if (password.length() < 8) {
                errPassword.setText("Minimum 8 caractères.");
                valid = false;
            } else if (!password.equals(confirm)) {
                errConfirm.setText("Les mots de passe ne correspondent pas.");
                valid = false;
            }
        }

        if (!valid) return;

        btnSave.setDisable(true);
        btnSave.setText("Enregistrement...");

        final int finalAge = age;
        final String finalPassword = password.isEmpty() ? user.getPassword() : password;
        final String finalPhoto = selectedPhotoPath != null ? selectedPhotoPath : user.getPhoto();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                user.setNom(nom);
                user.setPrenom(prenom);
                user.setEmail(email);
                user.setAge(finalAge);
                user.setPassword(finalPassword);
                user.setPhoto(finalPhoto);
                return userService.update(user);
            }
        };

        task.setOnSucceeded(e -> {
            btnSave.setDisable(false);
            btnSave.setText("ENREGISTRER");
            if (task.getValue()) {
                onBack();
            } else {
                lblError.setText("✗ Erreur lors de la mise à jour.");
            }
        });

        task.setOnFailed(e -> {
            btnSave.setDisable(false);
            btnSave.setText("ENREGISTRER");
            lblError.setText("✗ Erreur serveur.");
        });

        new Thread(task).start();
    }

    // ── Navigation ───────────────────────────────────────────────────
    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/ProfileView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnSave.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("Mon Profil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private void showPhoto(String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 90, 90, false, true);
                imgPhoto.setImage(img);
                imgPhoto.setVisible(true);
                lblAvatarEdit.setVisible(false);
            }
        } catch (Exception ignored) {}
    }

    private void clearErrors() {
        errNom.setText(""); errPrenom.setText(""); errEmail.setText("");
        errAge.setText(""); errPassword.setText(""); errConfirm.setText("");
        lblSuccess.setText(""); lblError.setText("");
    }

    private String getInitials(String nom, String prenom) {
        String n = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        return p + n;
    }
}