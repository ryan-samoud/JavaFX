package com.esports.controller;

import com.esports.interfaces.IUserService;
import com.esports.service.UserService;
import com.esports.model.User;

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
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * CONTROLLER — UsersController.java
 * Compatible DB pi_webjava
 */
public class UsersController implements Initializable {

    @FXML private Label lblUserCount;
    @FXML private TextField fieldSearch;
    @FXML private ComboBox<String> comboRoleFilter;

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colActive;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, Void> colActions;

    private final IUserService userService = new UserService();
    private ObservableList<User> masterList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        loadUsers();
    }

    // ═══════════════════════════════════════
    // COLUMNS
    // ═══════════════════════════════════════

    private void setupColumns() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // ⚠ DB = nom
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        colActive.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "✔ Actif" : "✘ Inactif"));

        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }

                setText(item);
                setStyle(item.startsWith("✔")
                        ? "-fx-text-fill: #00ff9d;"
                        : "-fx-text-fill: #ff4757;");
            }
        });

        colCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateCreation() != null
                                ? data.getValue().getDateCreation().format(FMT)
                                : "—"
                )
        );

        // ACTIONS
        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button btnDeactivate = new Button("Désactiver");
            private final HBox box = new HBox(8, btnDeactivate);

            {
                btnDeactivate.setStyle(
                        "-fx-text-fill: red; -fx-cursor: hand;"
                );

                btnDeactivate.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    onDeactivateUser(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ═══════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════

    private void setupFilters() {

        comboRoleFilter.setItems(FXCollections.observableArrayList(
                "Tous", "admin", "player", "spectateur"
        ));
        comboRoleFilter.setValue("Tous");

        filteredList = new FilteredList<>(masterList, p -> true);
        tableUsers.setItems(filteredList);

        fieldSearch.textProperty().addListener((o, a, b) -> applyFilter());
        comboRoleFilter.valueProperty().addListener((o, a, b) -> applyFilter());
    }

    // ═══════════════════════════════════════
    // LOAD DATA
    // ═══════════════════════════════════════

    private void loadUsers() {
        try {
            List<User> users = userService.findAll();
            masterList.setAll(users);
            lblUserCount.setText(users.size() + " utilisateurs");
        } catch (Exception e) {
            System.err.println("[UsersController] " + e.getMessage());
        }
    }

    private void applyFilter() {

        String search = fieldSearch.getText().toLowerCase();
        String role = comboRoleFilter.getValue();

        filteredList.setPredicate(u -> {

            boolean matchSearch =
                    u.getNom().toLowerCase().contains(search) ||
                            u.getEmail().toLowerCase().contains(search);

            boolean matchRole =
                    role.equals("Tous") || u.getRole().equals(role);

            return matchSearch && matchRole;
        });

        lblUserCount.setText(filteredList.size() + " utilisateurs affichés");
    }

    // ═══════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════

    @FXML
    private void onAddUser() {
        System.out.println("ADD USER (TODO)");
    }

    private void onDeactivateUser(User user) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Désactiver utilisateur");
        alert.setContentText("Désactiver " + user.getNom() + " ?");

        Optional<ButtonType> res = alert.showAndWait();

        if (res.isPresent() && res.get() == ButtonType.OK) {

            // ⚠ il faut que DAO supporte ça
            boolean ok = userService.deactivate(user.getId());

            if (ok) {
                masterList.remove(user);
            }
        }
    }
}