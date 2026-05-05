package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.interfaces.ITournamentInscriptionService;
import com.esports.model.Tournament;
import com.esports.model.TournamentInscription;
import com.esports.model.User;
import com.esports.service.TournamentService;
import com.esports.service.TournamentInscriptionService;
import com.esports.service.AuthService;
import com.esports.utils.AlertUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserInscriptionsController implements Initializable {

    @FXML private VBox inscriptionsList;

    private final ITournamentService tournamentService = new TournamentService();
    private final ITournamentInscriptionService inscriptionService = new TournamentInscriptionService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadInscriptions();
    }

    private void loadInscriptions() {
        inscriptionsList.getChildren().clear();
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) {
            Label placeholder = new Label("Veuillez vous connecter pour voir vos inscriptions.");
            placeholder.setStyle("-fx-text-fill: #f87171; -fx-font-size: 16px; -fx-font-style: italic;");
            inscriptionsList.getChildren().add(placeholder);
            return;
        }

        List<TournamentInscription> list = inscriptionService.findByPlayer(currentUser.getId());
        
        if (list.isEmpty()) {
            Label placeholder = new Label("Vous n'êtes inscrit à aucun tournoi.");
            placeholder.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 16px; -fx-font-style: italic;");
            inscriptionsList.getChildren().add(placeholder);
            return;
        }

        for (TournamentInscription ti : list) {
            Tournament t = tournamentService.findById(ti.getTournoiId());
            if (t != null) {
                inscriptionsList.getChildren().add(buildRow(ti, t));
            }
        }
    }

    private HBox buildRow(TournamentInscription ti, Tournament t) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20));
        row.setStyle("-fx-background-color: #111122; -fx-background-radius: 12; -fx-border-color: #3b2b5a; -fx-border-width: 1; -fx-border-radius: 12;");

        VBox info = new VBox(5);
        Label name = new Label(t.getNom()); name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label game = new Label("🎮 " + t.getJeu() + " • Inscrit le " + (ti.getCreatedAt() != null ? ti.getCreatedAt().toLocalDate() : "—")); 
        game.setStyle("-fx-text-fill: #7c6fa8; -fx-font-size: 12px;");
        info.getChildren().addAll(name, game);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnModify = new Button("Changer");
        btnModify.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffaa00; -fx-border-color: #ffaa00; -fx-border-radius: 5; -fx-cursor: hand; -fx-padding: 6 12;");
        btnModify.setOnAction(e -> handleModify(ti));

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757; -fx-border-color: #ff4757; -fx-border-radius: 5; -fx-cursor: hand; -fx-padding: 6 12;");
        btnCancel.setOnAction(e -> handleCancel(ti.getId()));

        row.getChildren().addAll(info, btnModify, btnCancel);
        return row;
    }

    private void handleCancel(int id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer l'annulation de cette inscription ?");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            if (inscriptionService.unregister(id)) {
                loadInscriptions();
                AlertUtils.showNotification((Stage) inscriptionsList.getScene().getWindow(), "Inscription annulée.", true);
            }
        }
    }

    private void handleModify(TournamentInscription ti) {
        List<Tournament> openTourneys = tournamentService.findAll().stream()
                .filter(t -> "OUVERT".equals(t.getStatut()) && t.getId() != ti.getTournoiId())
                .toList();
        
        if (openTourneys.isEmpty()) {
            showAlert("Désolé", "Aucun autre tournoi ouvert n'est disponible pour le moment.");
            return;
        }

        ChoiceDialog<Tournament> dialog = new ChoiceDialog<>(null, openTourneys);
        dialog.setTitle("Modifier l'inscription");
        dialog.setHeaderText("Transférer votre inscription");
        dialog.setContentText("Choisissez un nouveau tournoi :");
        
        dialog.showAndWait().ifPresent(newT -> {
            ti.setTournoiId(newT.getId());
            if (inscriptionService.update(ti)) {
                AlertUtils.showNotification((Stage) inscriptionsList.getScene().getWindow(), "Transfert réussi vers " + newT.getNom(), true);
                loadInscriptions();
            }
        });
    }

    @FXML
    private void onBack() {
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/com/esports/fxml/TournamentsPublicView.fxml"));
            inscriptionsList.getScene().setRoot(r);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}
