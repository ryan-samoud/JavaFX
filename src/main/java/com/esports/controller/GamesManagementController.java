package com.esports.controller;

import com.esports.model.CategorieJeu;
import com.esports.model.Jeu;
import com.esports.service.CategorieJeuService;
import com.esports.service.JeuService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GamesManagementController implements Initializable {

    @FXML private TextField fieldSearchCat, fieldSearchGame;

    // --- Games Section ---
    @FXML private TableView<Jeu> tableGames;
    @FXML private TableColumn<Jeu, String> colGameImage, colGameNom, colGameCat, colGameMode, colGameAge, colGameNote, colGamePlayers, colGameActions;

    // --- Categories Section ---
    @FXML private TableView<CategorieJeu> tableCats;
    @FXML private TableColumn<CategorieJeu, String> colCatId, colCatNom, colCatGenre, colCatActions;

    private final JeuService jeuService = new JeuService();
    private final CategorieJeuService catService = new CategorieJeuService();

    private ObservableList<Jeu> gamesList = FXCollections.observableArrayList();
    private ObservableList<CategorieJeu> catsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
        loadData();

        fieldSearchCat.textProperty().addListener((obs, oldVal, newVal) -> filterCategories());
        fieldSearchGame.textProperty().addListener((obs, oldVal, newVal) -> filterGames());
    }

    private void setupTables() {
        // --- CATEGORIES ---
        colCatId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colCatNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomCategorie()));
        colCatGenre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenre()));
        setupCatActions();

        // --- GAMES ---
        colGameImage.setCellValueFactory(data -> new SimpleStringProperty("🖼️")); // Placeholder symbol
        colGameNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        colGameCat.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategorie() != null ? data.getValue().getCategorie().getNomCategorie() : "N/A"
        ));
        colGameMode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMode()));
        colGameAge.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrancheAge() + "+"));
        colGameNote.setCellValueFactory(data -> new SimpleStringProperty("⭐ " + data.getValue().getNote()));
        colGamePlayers.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getNbJoueurs())));
        setupGameActions();
    }

    private void loadData() {
        gamesList.setAll(jeuService.findAll());
        catsList.setAll(catService.findAll());
        tableGames.setItems(gamesList);
        tableCats.setItems(catsList);
    }

    // ================= ACTIONS CATÉGORIES =================

    @FXML
    private void onOpenAddCat() {
        openCategoryPopup(null);
    }

    private void setupCatActions() {
        colCatActions.setCellFactory(param -> new TableCell<>() {
            private final ImageView ivEdit = createIcon("/icons/editer.png");
            private final ImageView ivDelete = createIcon("/icons/supprimer.png");
            private final Button btnEdit = new Button("", ivEdit);
            private final Button btnDelete = new Button("", ivDelete);
            private final HBox container = new HBox(12, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDelete.getStyleClass().add("btn-action-delete");
                btnEdit.setOnAction(e -> openCategoryPopup(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> {
                    CategorieJeu c = getTableView().getItems().get(getIndex());
                    if (confirmDelete("Supprimer la catégorie '" + c.getNomCategorie() + "' ?")) {
                        catService.delete(c.getId());
                        loadData();
                    }
                });
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void openCategoryPopup(CategorieJeu c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/CategoryFormView.fxml"));
            Parent root = loader.load();
            CategoryFormController controller = loader.getController();
            controller.setCategory(c);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED); 
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) loadData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= ACTIONS JEUX =================

    @FXML
    private void onOpenAddGame() {
        openGamePopup(null);
    }

    private void setupGameActions() {
        colGameActions.setCellFactory(param -> new TableCell<>() {
            private final ImageView ivEdit = createIcon("/icons/editer.png");
            private final ImageView ivDelete = createIcon("/icons/supprimer.png");
            private final Button btnEdit = new Button("Modifier", ivEdit);
            private final Button btnDelete = new Button("", ivDelete);
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDelete.getStyleClass().add("btn-action-delete");
                btnEdit.setOnAction(e -> openGamePopup(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> {
                    Jeu j = getTableView().getItems().get(getIndex());
                    if (confirmDelete("Supprimer le jeu '" + j.getNom() + "' ?")) {
                        jeuService.delete(j.getId());
                        loadData();
                    }
                });
                btnDelete.setTooltip(new Tooltip("Supprimer"));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void openGamePopup(Jeu j) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/GameFormView.fxml"));
            Parent root = loader.load();
            GameFormController controller = loader.getController();
            controller.setJeu(j);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) loadData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================= UTILS =================

    /** Crée une icône ImageView à partir du chemin et la rend blanche */
    private ImageView createIcon(String path) {
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            iv.setPreserveRatio(true);
            
            // Effet pour rendre l'icône noire en blanche
            ColorAdjust whiteEffect = new ColorAdjust();
            whiteEffect.setBrightness(1.0); // Monte la luminosité au max (noir -> blanc)
            iv.setEffect(whiteEffect);
            
            return iv;
        } catch (Exception e) {
            System.err.println("Impossible de charger l'icône: " + path);
            return new ImageView();
        }
    }

    private void filterCategories() {
        String q = fieldSearchCat.getText().toLowerCase();
        tableCats.setItems(FXCollections.observableArrayList(
            catsList.stream().filter(c -> {
                String n = c.getNomCategorie() != null ? c.getNomCategorie().toLowerCase() : "";
                return n.contains(q);
            }).collect(Collectors.toList())
        ));
    }

    private void filterGames() {
        String q = fieldSearchGame.getText().toLowerCase();
        tableGames.setItems(FXCollections.observableArrayList(
            gamesList.stream().filter(j -> {
                String n = j.getNom() != null ? j.getNom().toLowerCase() : "";
                return n.contains(q);
            }).collect(Collectors.toList())
        ));
    }

    private boolean confirmDelete(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/esports/css/dashboard.css").toExternalForm());
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
