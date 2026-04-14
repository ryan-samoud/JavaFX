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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CONTROLLER — UsersController.java
 * Gestion des utilisateurs : création, ban, suspension, débannissement.
 */
public class UsersController implements Initializable {

    // ── Top bar ──
    @FXML private Label            lblUserCount;
    @FXML private TextField        fieldSearch;
    @FXML private ComboBox<String> comboRoleFilter;
    @FXML private ComboBox<String> comboStatusFilter;

    // ── Table ──
    @FXML private TableView<User>             tableUsers;
    @FXML private TableColumn<User, Integer>  colId;
    @FXML private TableColumn<User, String>   colNom;
    @FXML private TableColumn<User, String>   colPrenom;
    @FXML private TableColumn<User, String>   colEmail;
    @FXML private TableColumn<User, String>   colRole;
    @FXML private TableColumn<User, String>   colActive;
    @FXML private TableColumn<User, String>   colCreatedAt;
    @FXML private TableColumn<User, Void>     colActions;


    private final IUserService userService = new UserService();
    private final ObservableList<User> masterList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ═══════════════════════════════════════
    // INIT
    // ═══════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        setupColumns();
        loadUsers();
    }

    // ═══════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════

    private void setupFilters() {
        comboRoleFilter.setItems(FXCollections.observableArrayList(
                "Tous", "admin", "player", "spectateur"));
        comboRoleFilter.setValue("Tous");

        comboStatusFilter.setItems(FXCollections.observableArrayList(
                "Tous", "Actif", "Suspendu", "Banni"));
        comboStatusFilter.setValue("Tous");

        filteredList = new FilteredList<>(masterList, p -> true);
        tableUsers.setItems(filteredList);

        fieldSearch.textProperty().addListener((o, a, b) -> applyFilter());
        comboRoleFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        comboStatusFilter.valueProperty().addListener((o, a, b) -> applyFilter());
    }

    // ═══════════════════════════════════════
    // COLUMNS
    // ═══════════════════════════════════════

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Role badge
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label badge = new Label(role.toUpperCase());
                badge.getStyleClass().add(
                        "admin".equalsIgnoreCase(role) ? "role-admin" : "role-user");
                setGraphic(badge);
                setText(null);
            }
        });

        // Status badge
        colActive.setCellValueFactory(data -> {
            User u = data.getValue();
            if (u.isSuspended()) return new SimpleStringProperty("SUSPENDU");
            if (u.isBanned())    return new SimpleStringProperty("BANNI");
            return new SimpleStringProperty("ACTIF");
        });
        colActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                badge.getStyleClass().add("status-badge");
                switch (status) {
                    case "ACTIF"     -> badge.getStyleClass().add("badge-active");
                    case "SUSPENDU"  -> badge.getStyleClass().add("status-in_game");
                    default          -> badge.getStyleClass().add("status-passed");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        // Date
        colCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateCreation() != null
                                ? data.getValue().getDateCreation().format(FMT)
                                : "—"));

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnBan     = new Button("Bannir");
            private final Button btnSuspend = new Button("Suspendre");
            private final Button btnUnban   = new Button("Débannir");
            private final HBox   box        = new HBox(6);

            {
                btnBan.getStyleClass().add("btn-danger");
                btnSuspend.getStyleClass().add("btn-secondary");
                btnUnban.getStyleClass().add("btn-primary");
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                int idx = getIndex();
                if (empty || idx < 0 || idx >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(idx);
                if (u == null) { setGraphic(null); return; }

                // Wire actions here so 'u' is in scope
                btnBan.setOnAction(e     -> onBanUser(u));
                btnSuspend.setOnAction(e -> onSuspendUser(u));
                btnUnban.setOnAction(e   -> onUnbanUser(u));

                box.getChildren().clear();
                if (u.isActive()) {
                    box.getChildren().addAll(btnBan, btnSuspend);
                } else {
                    box.getChildren().add(btnUnban);
                }
                setGraphic(box);
            }
        });
    }

    // ═══════════════════════════════════════
    // LOAD DATA
    // ═══════════════════════════════════════

    private void loadUsers() {
        try {
            List<User> users = userService.findAllUsers();
            masterList.setAll(users);
            lblUserCount.setText(users.size() + " utilisateur(s)");
        } catch (Exception e) {
            System.err.println("[UsersController] " + e.getMessage());
        }
    }

    private void applyFilter() {
        String search = fieldSearch.getText().toLowerCase();
        String role   = comboRoleFilter.getValue();
        String status = comboStatusFilter.getValue();

        filteredList.setPredicate(u -> {
            boolean matchSearch = u.getNom().toLowerCase().contains(search)
                    || u.getPrenom().toLowerCase().contains(search)
                    || u.getEmail().toLowerCase().contains(search);

            boolean matchRole = role == null || role.equals("Tous")
                    || u.getRole().equalsIgnoreCase(role);

            boolean matchStatus = switch (status == null ? "Tous" : status) {
                case "Actif"     -> u.isActive();
                case "Suspendu"  -> u.isSuspended();
                case "Banni"     -> u.isBanned();
                default          -> true;
            };

            return matchSearch && matchRole && matchStatus;
        });

        lblUserCount.setText(filteredList.size() + " utilisateur(s) affichés");
    }

    // ═══════════════════════════════════════
    // ADD USER — MODAL WINDOW
    // ═══════════════════════════════════════

    @FXML
    private void onAddUser() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);
        modal.setResizable(false);

        // ── Fields ──
        TextField     fNom      = field("Nom");
        TextField     fPrenom   = field("Prénom");
        TextField     fEmail    = field("Email");
        TextField     fAge      = field("Âge (défaut : 18)");
        PasswordField fPassword = new PasswordField();
        fPassword.setPromptText("Mot de passe (min. 6 car.)");
        fPassword.getStyleClass().add("input-field");

        ComboBox<String> fRole = new ComboBox<>(
                FXCollections.observableArrayList("admin", "player", "spectateur"));
        fRole.setPromptText("Choisir un rôle");
        fRole.setMaxWidth(Double.MAX_VALUE);
        fRole.getStyleClass().add("combo-filter");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        lblError.setWrapText(true);

        // ── Header ──
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, #3b1f6b, #6b1f4a);" +
                        "-fx-border-color: transparent transparent rgba(139,92,246,0.3) transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        Label ico   = new Label("👤");
        ico.setStyle("-fx-font-size: 22px;");
        Label title = new Label("  Créer un compte");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c6fa8;" +
                          "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        btnClose.setOnAction(e -> modal.close());

        header.getChildren().addAll(ico, title, spacer, btnClose);

        // ── Form grid ──
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(28, 32, 10, 32));
        grid.setStyle("-fx-background-color: #14112b;");

        grid.add(kpiLabel("NOM *"),           0, 0); grid.add(fNom,      1, 0);
        grid.add(kpiLabel("PRÉNOM *"),        0, 1); grid.add(fPrenom,   1, 1);
        grid.add(kpiLabel("EMAIL *"),         0, 2); grid.add(fEmail,    1, 2);
        grid.add(kpiLabel("ÂGE"),             0, 3); grid.add(fAge,      1, 3);
        grid.add(kpiLabel("RÔLE *"),          0, 4); grid.add(fRole,     1, 4);
        grid.add(kpiLabel("MOT DE PASSE *"),  0, 5); grid.add(fPassword, 1, 5);
        grid.add(lblError,                    0, 6, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(260);
        grid.getColumnConstraints().addAll(c0, c1);

        // ── Footer buttons ──
        Button btnCreate = new Button("💾  Créer le compte");
        btnCreate.getStyleClass().add("btn-primary");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnCreate.setPrefHeight(42);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c6fa8;" +
                           "-fx-border-color: rgba(124,111,168,0.3); -fx-border-width: 1px;" +
                           "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                           "-fx-padding: 10 20 10 20; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        HBox footer = new HBox(12, btnCreate, btnCancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 32, 28, 32));
        footer.setStyle("-fx-background-color: #14112b;");
        HBox.setHgrow(btnCreate, Priority.ALWAYS);

        btnCreate.setOnAction(e -> {
            lblError.setText("");
            String nom    = fNom.getText().trim();
            String prenom = fPrenom.getText().trim();
            String email  = fEmail.getText().trim();
            String pass   = fPassword.getText();
            String role   = fRole.getValue();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()
                    || pass.isEmpty() || role == null) {
                lblError.setText("Tous les champs marqués * sont obligatoires.");
                return;
            }
            if (pass.length() < 6) {
                lblError.setText("Mot de passe : minimum 6 caractères.");
                return;
            }
            int age = 18;
            try { age = Integer.parseInt(fAge.getText().trim()); }
            catch (NumberFormatException ignored) {}

            if (userService.save(new User(nom, prenom, email, age, role, pass))) {
                modal.close();
                loadUsers();
                showInfo("Compte créé avec succès !");
            } else {
                lblError.setText("Erreur : cet email est peut-être déjà utilisé.");
            }
        });

        // ── Assemble ──
        VBox root = new VBox(header, grid, footer);
        root.setStyle("-fx-background-color: #14112b;" +
                      "-fx-border-color: rgba(139,92,246,0.4);" +
                      "-fx-border-width: 1.5;" +
                      "-fx-border-radius: 14;" +
                      "-fx-background-radius: 14;" +
                      "-fx-effect: dropshadow(gaussian, rgba(168,85,247,0.5), 40, 0.2, 0, 0);");

        String css = getClass().getResource("/com/esports/css/dashboard.css").toExternalForm();
        Scene scene = new Scene(root, 560, 520);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(css);

        modal.setScene(scene);
        modal.centerOnScreen();
        modal.showAndWait();
    }

    // ═══════════════════════════════════════
    // BAN
    // ═══════════════════════════════════════

    private void onBanUser(User user) {
        if (user == null) return;
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Bannir l'utilisateur");
        d.setHeaderText("Bannir " + user.getNom() + " " + user.getPrenom());
        d.setContentText("Raison du bannissement :");
        styleDialog(d.getDialogPane());

        d.showAndWait().ifPresent(reason -> {
            if (reason.trim().isEmpty()) return;
            if (userService.ban(user.getId(), reason.trim())) {
                loadUsers();
                showInfo("Utilisateur banni.");
            } else {
                showError("Erreur lors du bannissement.");
            }
        });
    }

    // ═══════════════════════════════════════
    // UNBAN
    // ═══════════════════════════════════════

    private void onUnbanUser(User user) {
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Réactiver le compte");
        confirm.setHeaderText(null);
        confirm.setContentText("Réactiver le compte de " + user.getNom() + " " + user.getPrenom() + " ?");

        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            if (userService.unban(user.getId())) {
                loadUsers();
                showInfo("Compte réactivé.");
            } else {
                showError("Erreur lors de la réactivation.");
            }
        });
    }

    // ═══════════════════════════════════════
    // SUSPEND
    // ═══════════════════════════════════════

    private void onSuspendUser(User user) {
        if (user == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Suspendre l'utilisateur");
        dialog.setHeaderText(null);

        ButtonType btnOk = new ButtonType("Suspendre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        ComboBox<String> comboDur = new ComboBox<>(FXCollections.observableArrayList(
                "1 jour", "3 jours", "7 jours", "14 jours", "30 jours"));
        comboDur.setValue("7 jours");
        comboDur.setMaxWidth(Double.MAX_VALUE);
        comboDur.getStyleClass().add("combo-filter");

        TextField fReason = new TextField();
        fReason.setPromptText("Raison de la suspension");
        fReason.getStyleClass().add("input-field");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");

        Label lblHeader = new Label("Suspendre : " + user.getNom() + " " + user.getPrenom());
        lblHeader.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #14112b;");
        grid.setMinWidth(380);

        Label lDur    = kpiLabel("DURÉE");
        Label lReason = kpiLabel("RAISON");

        grid.add(lblHeader, 0, 0, 2, 1);
        grid.add(lDur,      0, 1); grid.add(comboDur, 1, 1);
        grid.add(lReason,   0, 2); grid.add(fReason,  1, 2);
        grid.add(lblErr,    0, 3, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(80);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        dialog.getDialogPane().setContent(grid);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(bt -> {
            if (bt != btnOk) return null;
            String reason = fReason.getText().trim();
            if (reason.isEmpty()) { lblErr.setText("La raison est obligatoire."); return null; }

            int days = switch (comboDur.getValue()) {
                case "1 jour"   -> 1;
                case "3 jours"  -> 3;
                case "14 jours" -> 14;
                case "30 jours" -> 30;
                default          -> 7;
            };
            LocalDateTime until = LocalDateTime.now().plusDays(days);

            if (userService.suspend(user.getId(), until, reason)) {
                loadUsers();
                showInfo("Suspendu jusqu'au " + until.format(FMT) + ".");
            } else {
                showError("Erreur lors de la suspension.");
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    private TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("input-field");
        return f;
    }

    private void styleDialog(DialogPane pane) {
        pane.setStyle("-fx-background-color: #14112b;");
    }

    private Label kpiLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("kpi-title");
        return l;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
