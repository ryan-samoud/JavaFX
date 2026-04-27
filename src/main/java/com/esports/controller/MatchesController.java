package com.esports.controller;

import com.esports.interfaces.IMatchService;
import com.esports.model.Match;
import com.esports.service.MatchService;
import com.esports.utils.AlertUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MatchesController implements Initializable {

    @FXML private Label lblMatchCount;
    @FXML private TextField fieldSearch;
    @FXML private ComboBox<String> comboSortOrder;
    @FXML private FlowPane matchesContainer;

    private final IMatchService matchService = new MatchService();
    private List<Match> allMatches;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fieldSearch.textProperty().addListener((obs, ov, nv) -> applyFilter());
        comboSortOrder.setValue("Date (Décroissant)");
        comboSortOrder.valueProperty().addListener((o, ov, nv) -> applyFilter());
        loadMatches();
    }

    private void loadMatches() {
        allMatches = matchService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String search = fieldSearch.getText().toLowerCase().trim();
        List<Match> filtered = allMatches.stream()
                .filter(m -> search.isEmpty() 
                        || (m.getNomJoueur1() != null && m.getNomJoueur1().toLowerCase().contains(search)) 
                        || (m.getNomJoueur2() != null && m.getNomJoueur2().toLowerCase().contains(search)) 
                        || (m.getRound() != null && m.getRound().toLowerCase().contains(search)))
                .collect(Collectors.toList());

        if ("Date (Croissant)".equals(comboSortOrder.getValue())) {
            filtered.sort(Comparator.comparing(Match::getDateMatch, Comparator.nullsLast(Comparator.naturalOrder())));
        } else {
            filtered.sort(Comparator.comparing(Match::getDateMatch, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        }

        renderScoreboards(filtered);
        lblMatchCount.setText(filtered.size() + " match(s) trouvé(s)");
    }

    private void renderScoreboards(List<Match> list) {
        matchesContainer.getChildren().clear();
        for (Match m : list) matchesContainer.getChildren().add(createScoreboardCard(m));
    }

    private VBox createScoreboardCard(Match m) {
        VBox card = new VBox(15); card.setPrefWidth(380); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #111122; -fx-background-radius: 12; -fx-border-color: #1a1a2e; -fx-border-width: 2; -fx-border-radius: 12;");
        
        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        Label round = new Label(m.getRound() != null ? m.getRound().toUpperCase() : "ROUND");
        round.setStyle("-fx-text-fill: #ffaa00; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-color: rgba(255,170,0,0.1); -fx-padding: 3 8; -fx-background-radius: 4;");
        
        Label dateLabel = new Label(m.getDateMatch() != null ? m.getDateMatch().format(DATE_FMT) : "");
        dateLabel.setStyle("-fx-text-fill: #7c6fa8; -fx-font-size: 11px;");
        header.getChildren().addAll(round, dateLabel);
        
        HBox scoreRow = new HBox(15); scoreRow.setAlignment(Pos.CENTER);
        scoreRow.getChildren().addAll(
            buildPlayerBox(m.getNomJoueur1(), m.getScoreJoueur1(), "#00b8ff"),
            new Label("VS"),
            buildPlayerBox(m.getNomJoueur2(), m.getScoreJoueur2(), "#00ff9d")
        );
        
        HBox actions = new HBox(12); actions.setAlignment(Pos.CENTER);
        Button ed = new Button("Editer"), de = new Button("Suppr");
        ed.setStyle("-fx-background-color: transparent; -fx-text-fill: #00b8ff; -fx-border-color: #00b8ff; -fx-border-radius: 5; -fx-cursor: hand;");
        de.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4757; -fx-border-color: #ff4757; -fx-border-radius: 5; -fx-cursor: hand;");
        ed.setOnAction(e -> onEditMatch(m));
        de.setOnAction(e -> onDeleteMatch(m));
        actions.getChildren().addAll(ed, de);
        
        card.getChildren().addAll(header, scoreRow, actions);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #161630; -fx-border-color: #ffaa00; -fx-border-width: 2; -fx-border-radius: 12;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #111122; -fx-border-color: #1a1a2e; -fx-border-width: 2; -fx-border-radius: 12;"));
        return card;
    }

    private VBox buildPlayerBox(String name, int score, String color) {
        VBox box = new VBox(8); box.setAlignment(Pos.CENTER); box.setPrefWidth(120);
        Label n = new Label(name != null ? name : "—");
        n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label s = new Label(String.valueOf(score));
        s.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: bold; -fx-font-family: 'Impact';");
        box.getChildren().addAll(n, s);
        return box;
    }

    @FXML private void onShowStats() {
        Stage s = new Stage(); CategoryAxis x = new CategoryAxis(); NumberAxis y = new NumberAxis();
        BarChart<String, Number> bc = new BarChart<>(x, y); XYChart.Series<String, Number> series = new XYChart.Series<>();
        allMatches.stream().collect(Collectors.groupingBy(Match::getTournoiId, Collectors.counting()))
                .forEach((tid, count) -> series.getData().add(new XYChart.Data<>("#" + tid, count)));
        bc.getData().add(series); s.setScene(new Scene(new VBox(bc), 800, 500)); s.show();
    }

    @FXML private void onAddMatch() { openMatchForm(null); }
    private void onEditMatch(Match m) { openMatchForm(m); }

    private void openMatchForm(Match m) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/com/esports/fxml/MatchForm.fxml"));
            Parent r = l.load(); 
            ((MatchFormController) l.getController()).setMatch(m);
            Stage s = new Stage(); s.setScene(new Scene(r)); s.showAndWait();
            if (((MatchFormController) l.getController()).isSaved()) {
                loadMatches();
                AlertUtils.showNotification((Stage) matchesContainer.getScene().getWindow(), "Match enregistré !", true);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void onDeleteMatch(Match m) {
        if (new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce match ?").showAndWait().orElse(null) == ButtonType.OK) {
            if (matchService.delete(m.getId())) {
                loadMatches();
                AlertUtils.showNotification((Stage) matchesContainer.getScene().getWindow(), "Match supprimé !", true);
            } else {
                AlertUtils.showNotification((Stage) matchesContainer.getScene().getWindow(), "Erreur de suppression", false);
            }
        }
    }
}
