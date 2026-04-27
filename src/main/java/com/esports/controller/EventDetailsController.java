package com.esports.controller;

import com.esports.model.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;

public class EventDetailsController {

    @FXML private Label lblNom, lblLieu, lblDate, lblDesc, lblParticipants;
    @FXML private ImageView imageView;

    public void setEvent(Evenement e) {
        lblNom.setText(e.getNom());
        lblLieu.setText("📍 " + e.getLieu());
        lblDate.setText("📅 " + e.getDate());
        lblDesc.setText(e.getDescription());
        lblParticipants.setText("👥 " + e.getNbrParticipant());

        try {
            Image img = new Image(
                    new File("src/main/resources/images/events/" + e.getImage())
                            .toURI().toString()
            );
            imageView.setImage(img);
        } catch (Exception ex) {
            System.out.println("Image not found");
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }
}