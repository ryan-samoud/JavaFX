package com.esports.controller;

import com.esports.model.Commande;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class CommandeSuccesController {

    @FXML private Label lblReference;
    @FXML private Label lblDate;
    @FXML private Label lblModePaiement;
    @FXML private Label lblAdresse;
    @FXML private Label lblTotal;
    @FXML private Label lblStatut;

    public void setCommande(Commande commande) {
        lblReference.setText(commande.getReference());
        lblDate.setText(commande.getDateCommande()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblModePaiement.setText(commande.getModePaiement());
        lblAdresse.setText(commande.getAdresseLivraison());
        lblTotal.setText(String.format("%.0f DT", commande.getMontantTotal()));
        lblStatut.setText("● Paiement validé");
    }

    @FXML
    private void onContinuer() {
        navigateTo("/com/esports/fxml/ShopPublicView.fxml", "Boutique");
    }

    @FXML
    private void onAccueil() {
        navigateTo("/com/esports/fxml/HomeView.fxml", "NexUS Gaming Arena");
    }

    private void navigateTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) lblReference.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}














































