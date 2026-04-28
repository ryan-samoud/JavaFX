package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.service.CategorieJeuService;
import com.esports.service.CloudinaryUploadService;
import com.esports.service.JeuService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class GameFormController implements Initializable {

    @FXML private Label lblHeaderTitle;
    @FXML private TextField fieldNom, fieldAge, fieldPlayers, fieldNote, fieldImage;
    @FXML private ComboBox<String> comboMode;
    @FXML private ComboBox<CategorieJeu> comboCat;
    @FXML private TextArea fieldDesc;
    
    @FXML private Label errNom, errAge, errMode, errCat, errPlayers, errNote;
    @FXML private Button btnSubmit;

    private final JeuService jeuService = new JeuService();
    private final CategorieJeuService catService = new CategorieJeuService();
    private final CloudinaryUploadService cloudinaryUploadService = new CloudinaryUploadService();
    
    private Jeu jeuToEdit = null;
    private boolean saved = false;

    // Bad words list (sample)
    private static final List<String> BAD_WORDS = Arrays.asList("merde", "putain", "con", "salope", "fuck", "shit", "bastard");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboMode.getItems().addAll("solo", "multi", "coop");
        comboCat.getItems().setAll(catService.findAll());
        
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        fieldNom.textProperty().addListener((o, a, b) -> hideError(errNom, fieldNom));
        fieldAge.textProperty().addListener((o, a, b) -> hideError(errAge, fieldAge));
        fieldPlayers.textProperty().addListener((o, a, b) -> hideError(errPlayers, fieldPlayers));
        fieldNote.textProperty().addListener((o, a, b) -> hideError(errNote, fieldNote));
        comboMode.valueProperty().addListener((o, a, b) -> hideError(errMode, comboMode));
        comboCat.valueProperty().addListener((o, a, b) -> hideError(errCat, comboCat));
    }

    public void setJeu(Jeu j) {
        this.jeuToEdit = j;
        if (j != null) {
            lblHeaderTitle.setText("Modifier le Jeu : " + j.getNom());
            fieldNom.setText(j.getNom());
            fieldAge.setText(String.valueOf(j.getTrancheAge()));
            comboMode.setValue(j.getMode());
            fieldPlayers.setText(String.valueOf(j.getNbJoueurs()));
            fieldNote.setText(String.valueOf(j.getNote()));
            fieldImage.setText(j.getImage());
            fieldDesc.setText(j.getDescription());
            
            for(CategorieJeu c : comboCat.getItems()) {
                if (c.getId() == j.getCategorieId()) {
                    comboCat.setValue(c);
                    break;
                }
            }
        }
    }

    @FXML
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner l'image du jeu");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File file = fileChooser.showOpenDialog(fieldImage.getScene().getWindow());
        if (file == null) {
            return;
        }

        fieldImage.setText("Uploading to Cloudinary...");
        btnSubmit.setDisable(true);

        Task<String> uploadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return cloudinaryUploadService.uploadImage(file);
            }
        };

        uploadTask.setOnSucceeded(e -> {
            fieldImage.setText(uploadTask.getValue());
            btnSubmit.setDisable(false);
            new Alert(Alert.AlertType.INFORMATION, "Image uploaded to Cloudinary successfully.", ButtonType.OK).showAndWait();
        });
        uploadTask.setOnFailed(e -> {
            Throwable ex = uploadTask.getException();
            fieldImage.setText("");
            btnSubmit.setDisable(false);
            new Alert(Alert.AlertType.ERROR,
                    "Cloudinary upload failed: " + (ex != null ? ex.getMessage() : "unknown error"),
                    ButtonType.OK).showAndWait();
        });

        Thread thread = new Thread(uploadTask, "cloudinary-upload-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onSubmit() {
        if (!validate()) return;

        Jeu j = (jeuToEdit == null) ? new Jeu() : jeuToEdit;
        j.setNom(fieldNom.getText().trim());
        j.setTrancheAge(Integer.parseInt(fieldAge.getText().trim()));
        j.setMode(comboMode.getValue());
        j.setCategorieId(comboCat.getValue().getId());
        j.setNbJoueurs(Integer.parseInt(fieldPlayers.getText().trim()));
        j.setNote(Double.parseDouble(fieldNote.getText().trim()));
        j.setImage(fieldImage.getText());
        j.setDescription(fieldDesc.getText().trim());

        if (jeuToEdit == null) {
            jeuService.add(j);
        } else {
            jeuService.update(j);
        }

        saved = true;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    public boolean isSaved() { return saved; }

    private boolean validate() {
        boolean ok = true;
        
        // Nom
        if (fieldNom.getText().trim().isEmpty()) {
            ok = showError(errNom, fieldNom, "Le nom est obligatoire");
        }
        
        // Age
        String ageStr = fieldAge.getText().trim();
        if (ageStr.isEmpty() || !isNumeric(ageStr)) {
            ok = showError(errAge, fieldAge, "Âge invalide (Nombre requis)");
        } else if (Integer.parseInt(ageStr) < 3 || Integer.parseInt(ageStr) > 99) {
            ok = showError(errAge, fieldAge, "Âge entre 3 et 99");
        }

        // Mode
        if (comboMode.getValue() == null) {
            ok = showError(errMode, comboMode, "Choix obligatoire");
        }

        // Catégorie
        if (comboCat.getValue() == null) {
            ok = showError(errCat, comboCat, "Catégorie obligatoire");
        }

        // Joueurs
        String playersStr = fieldPlayers.getText().trim();
        if (playersStr.isEmpty() || !isNumeric(playersStr) || Integer.parseInt(playersStr) <= 0) {
            ok = showError(errPlayers, fieldPlayers, "Nombre de joueurs > 0");
        }

        // Note
        String noteStr = fieldNote.getText().trim();
        if (noteStr.isEmpty() || !isDouble(noteStr)) {
            ok = showError(errNote, fieldNote, "Note entre 0.0 et 5.0");
        }

        // Description / Bad words
        String desc = fieldDesc.getText().toLowerCase();
        for (String word : BAD_WORDS) {
            if (desc.contains(word)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "La description contient un langage inapproprié ("+word+").", ButtonType.OK);
                alert.showAndWait();
                fieldDesc.setStyle("-fx-border-color: #f87171; -fx-border-width: 2;");
                return false;
            }
        }
        
        return ok;
    }

    private boolean showError(Label lbl, Control field, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        if (field != null) field.setStyle("-fx-border-color: #f87171; -fx-border-width: 1.5; -fx-border-radius: 8;");
        return false;
    }

    private void hideError(Label lbl, Control field) {
        lbl.setVisible(false);
        lbl.setManaged(false);
        if (field != null) field.setStyle("");
    }

    private boolean isNumeric(String s) {
        try { Integer.parseInt(s.trim()); return true; } catch (Exception e) { return false; }
    }

    private boolean isDouble(String s) {
        try { 
            double d = Double.parseDouble(s.trim()); 
            return d >= 0 && d <= 5; 
        } catch (Exception e) { return false; }
    }

    private void close() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }
}
