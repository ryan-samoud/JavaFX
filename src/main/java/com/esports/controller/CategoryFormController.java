package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.service.CategorieJeuService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CategoryFormController implements Initializable {

    @FXML private Label lblHeaderTitle;
    @FXML private TextField fieldNom, fieldGenre;
    @FXML private Label errNom;
    @FXML private Button btnSubmit;

    private final CategorieJeuService catService = new CategorieJeuService();
    private CategorieJeu catToEdit = null;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fieldNom.textProperty().addListener((o, a, b) -> {
            errNom.setVisible(false);
            fieldNom.setStyle("");
        });
    }

    public void setCategory(CategorieJeu c) {
        this.catToEdit = c;
        if (c != null) {
            lblHeaderTitle.setText("Modifier la Catégorie");
            fieldNom.setText(c.getNomCategorie());
            fieldGenre.setText(c.getGenre());
        }
    }

    @FXML
    private void onSubmit() {
        String nom = fieldNom.getText().trim();
        if (nom.isEmpty()) {
            errNom.setVisible(true);
            fieldNom.setStyle("-fx-border-color: #f87171; -fx-border-width: 1.5;");
            return;
        }

        CategorieJeu c = (catToEdit == null) ? new CategorieJeu() : catToEdit;
        c.setNomCategorie(nom);
        c.setGenre(fieldGenre.getText().trim());

        boolean ok = catToEdit == null ? catService.add(c) : catService.update(c);
        if (!ok) {
            new Alert(AlertType.ERROR,
                    "Enregistrement impossible (nom de catégorie déjà utilisé, ou erreur SQL). "
                            + "Consultez la console pour le détail.")
                    .showAndWait();
            return;
        }

        saved = true;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    public boolean isSaved() { return saved; }

    private void close() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }
}
