package com.esports.controller;

import com.esports.interfaces.ITournamentService;
import com.esports.service.TournamentService;
import com.esports.model.Tournament;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CONTROLLER — TournamentsController.java
 * CRUD complet sur les tournois.
 */
public class TournamentsController implements Initializable {

    @FXML private Label     lblTournamentCount;
    @FXML private TextField fieldSearch;
    @FXML private ComboBox<String> comboStatusFilter;

    @FXML private TableView<Tournament>              tableTournaments;
    @FXML private TableColumn<Tournament, Integer>   colId;
    @FXML private TableColumn<Tournament, String>    colTitle;
    @FXML private TableColumn<Tournament, String>    colGame;
    @FXML private TableColumn<Tournament, String>    colPrize;
    @FXML private TableColumn<Tournament, Integer>   colMaxTeams;
    @FXML private TableColumn<Tournament, String>    colStatus;
    @FXML private TableColumn<Tournament, String>    colStartDate;
    @FXML private TableColumn<Tournament, Void>      colActions;

    private final ITournamentService tournamentService = new TournamentService();
    private ObservableList<Tournament> masterList   = FXCollections.observableArrayList();
    private FilteredList<Tournament>   filteredList;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        loadTournaments();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colGame.setCellValueFactory(new PropertyValueFactory<>("game"));
        colPrize.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.0f €", data.getValue().getPrizePool())));
        colMaxTeams.setCellValueFactory(new PropertyValueFactory<>("maxTeams"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStartDate() != null
                        ? data.getValue().getStartDate().format(FMT) : "—"));

        // Badge statut coloré
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "IN_PROGRESS" -> "#00ff9d";
                    case "OPEN"        -> "#00b8ff";
                    case "UPCOMING"    -> "#ff6b35";
                    case "FINISHED"    -> "#555566";
                    default            -> "#888899";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
            }
        });

        // Colonne actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Éditer");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #00b8ff;" +
                        "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                        "-fx-border-color: #00b8ff; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757;" +
                        "-fx-font-family: 'Courier New'; -fx-font-size: 11px;" +
                        "-fx-border-color: #ff4757; -fx-border-width: 1; -fx-border-radius: 4;" +
                        "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-cursor: hand;");

                btnEdit.setOnAction(e -> onEditTournament(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDeleteTournament(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupFilters() {
        comboStatusFilter.setItems(FXCollections.observableArrayList(
                "Tous", "UPCOMING", "OPEN", "IN_PROGRESS", "FINISHED"));
        comboStatusFilter.setValue("Tous");
        filteredList = new FilteredList<>(masterList, p -> true);
        tableTournaments.setItems(filteredList);
        fieldSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        comboStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void loadTournaments() {
        try {
            List<Tournament> list = tournamentService.findAll();
            masterList.setAll(list);
            lblTournamentCount.setText(list.size() + " tournoi(s)");
        } catch (Exception e) {
            System.err.println("[TournamentsController] " + e.getMessage());
        }
    }

    private void applyFilter() {
        String search = fieldSearch.getText().toLowerCase().trim();
        String status = comboStatusFilter.getValue();
        filteredList.setPredicate(t -> {
            boolean matchSearch = search.isEmpty()
                    || t.getTitle().toLowerCase().contains(search)
                    || t.getGame().toLowerCase().contains(search);
            boolean matchStatus = "Tous".equals(status) || status == null || t.getStatus().equals(status);
            return matchSearch && matchStatus;
        });
        lblTournamentCount.setText(filteredList.size() + " tournoi(s) affiché(s)");
    }

    @FXML
    private void onAddTournament() {
        // TODO : ouvrir formulaire d'ajout
        showInfo("Formulaire de création de tournoi à implémenter.");
    }

    private void onEditTournament(Tournament t) {
        showInfo("Éditer : " + t.getTitle());
        // TODO : ouvrir dialog d'édition pré-rempli
    }

    private void onDeleteTournament(Tournament t) {
        boolean confirmed = com.esports.utils.NexusDialog.showConfirm(
            "Supprimer le tournoi",
            "Supprimer « " + t.getTitle() + " » ?",
            "Cette action est irréversible."
        );
        if (confirmed) {
            if (tournamentService.delete(t.getId())) {
                masterList.remove(t);
                lblTournamentCount.setText(masterList.size() + " tournoi(s)");
            }
        }
    }

    private void showInfo(String msg) {
        com.esports.utils.NexusDialog.showInfo("Information", msg);
    }
}
