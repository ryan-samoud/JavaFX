package com.esports.controller;

import com.esports.model.User;
import com.esports.service.UserService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegisterController implements Initializable {

    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldEmail;
    @FXML private TextField     fieldAge;
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldConfirm;
    @FXML private RadioButton   rbJoueur;
    @FXML private RadioButton   rbSpectateur;

    @FXML private Label errNom;
    @FXML private Label errPrenom;
    @FXML private Label errEmail;
    @FXML private Label errAge;
    @FXML private Label errPassword;
    @FXML private Label errConfirm;
    @FXML private Label errRole;

    private final ToggleGroup roleGroup = new ToggleGroup();

    @FXML private Label  lblSuccess;
    @FXML private Label  lblError;
    @FXML private Button btnRegister;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rbJoueur.setToggleGroup(roleGroup);
        rbSpectateur.setToggleGroup(roleGroup);
        rbJoueur.setSelected(true);
        clearAllErrors();
    }

    // ── INSCRIPTION ─────────────────────────────────────────────────
    @FXML
    private void onRegister() {

        clearAllErrors();

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String ageStr   = fieldAge.getText().trim();
        String password = fieldPassword.getText();
        String confirm  = fieldConfirm.getText();
        String role     = rbSpectateur.isSelected() ? "spectateur" : "joueur";

        boolean valid = true;

        // Nom
        if (nom.isEmpty()) {
            errNom.setText("Le nom est obligatoire.");
            valid = false;
        } else if (nom.length() < 2) {
            errNom.setText("Minimum 2 caractères.");
            valid = false;
        }

        // Prénom
        if (prenom.isEmpty()) {
            errPrenom.setText("Le prénom est obligatoire.");
            valid = false;
        } else if (prenom.length() < 2) {
            errPrenom.setText("Minimum 2 caractères.");
            valid = false;
        }

        // Email
        if (email.isEmpty()) {
            errEmail.setText("L'email est obligatoire.");
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errEmail.setText("Format email invalide.");
            valid = false;
        }

        // Âge
        int age = 0;
        if (ageStr.isEmpty()) {
            errAge.setText("L'âge est obligatoire.");
            valid = false;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age < 13) {
                    errAge.setText("Vous devez avoir au moins 13 ans.");
                    valid = false;
                } else if (age > 120) {
                    errAge.setText("Âge invalide.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errAge.setText("L'âge doit être un nombre.");
                valid = false;
            }
        }

        // Mot de passe
        if (password.isEmpty()) {
            errPassword.setText("Le mot de passe est obligatoire.");
            valid = false;
        } else if (password.length() < 8) {
            errPassword.setText("Minimum 8 caractères.");
            valid = false;
        }

        // Confirmation
        if (confirm.isEmpty()) {
            errConfirm.setText("Veuillez confirmer le mot de passe.");
            valid = false;
        } else if (!confirm.equals(password)) {
            errConfirm.setText("Les mots de passe ne correspondent pas.");
            valid = false;
        }

        if (!valid) return;

        // ── Envoi async ─────────────────────────────────────────────
        btnRegister.setDisable(true);
        btnRegister.setText("Inscription...");

        final int finalAge = age;

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Vérifier si l'email existe déjà
                if (userService.findByEmail(email).isPresent()) {
                    return "EMAIL_EXISTE";
                }
                User newUser = new User(nom, prenom, email, finalAge, role, password);
                boolean ok = userService.save(newUser);
                return ok ? "OK" : "ERREUR";
            }
        };

        task.setOnSucceeded(e -> {
            btnRegister.setDisable(false);
            btnRegister.setText("S'INSCRIRE");

            switch (task.getValue()) {
                case "OK" -> {
                    clearForm();
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Compte créé");
                    success.setHeaderText("Inscription réussie !");
                    success.setContentText("Votre compte a été créé avec succès. Vous allez être redirigé vers la page de connexion.");
                    success.showAndWait();
                    onGoLogin();
                }
                case "EMAIL_EXISTE" -> errEmail.setText("Cet email est déjà utilisé.");
                default -> lblError.setText("✗ Erreur lors de l'inscription. Réessayez.");
            }
        });

        task.setOnFailed(e -> {
            btnRegister.setDisable(false);
            btnRegister.setText("S'INSCRIRE");
            lblError.setText("✗ Erreur serveur.");
        });

        new Thread(task).start();
    }

    // ── NAVIGATION ──────────────────────────────────────────────────
    @FXML
    private void onGoLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRegister.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("NEXUS ESPORTS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── HELPERS ─────────────────────────────────────────────────────
    private void clearAllErrors() {
        errNom.setText("");      errPrenom.setText("");
        errEmail.setText("");    errAge.setText("");
        errPassword.setText(""); errConfirm.setText("");
        errRole.setText("");
        lblSuccess.setText("");  lblError.setText("");
    }

    private void clearForm() {
        fieldNom.clear();     fieldPrenom.clear();
        fieldEmail.clear();   fieldAge.clear();
        fieldPassword.clear(); fieldConfirm.clear();
    }
}