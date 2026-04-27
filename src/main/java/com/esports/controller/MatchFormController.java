package com.esports.controller;

import com.esports.interfaces.IMatchService;
import com.esports.interfaces.ITournamentService;
import com.esports.model.Match;
import com.esports.model.Tournament;
import com.esports.service.MatchService;
import com.esports.service.TournamentService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MatchFormController {

    @FXML private Label lblTitle;
    @FXML private ComboBox<Tournament> comboTournoi;
    @FXML private TextField fieldRound;
    @FXML private DatePicker datePicker;
    @FXML private TextField fieldTime;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextField fieldJoueur1;
    @FXML private TextField fieldScore1;
    @FXML private TextField fieldJoueur2;
    @FXML private TextField fieldScore2;
    @FXML private Button btnSave;

    @FXML private Label errRound;
    @FXML private Label errDate;
    @FXML private Label errJoueur1;
    @FXML private Label errJoueur2;
    @FXML private Label errScores;

    private final IMatchService matchService = new MatchService();
    private final ITournamentService tournamentService = new TournamentService();
    private Match currentMatch;
    private boolean saved = false;

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList("PLANIFIE", "EN_COURS", "TERMINE"));
        comboStatut.setValue("PLANIFIE");
        loadTournaments();
        datePicker.setValue(LocalDate.now());
        fieldTime.setText("14:00");
        setupValidation();
    }

    private void setupValidation() {
        fieldRound.textProperty().addListener((o, old, n) -> {
            boolean invalid = n.length() < 3;
            errRound.setVisible(invalid);
            fieldRound.setStyle(invalid ? "-fx-border-color: #ff4757;" : "-fx-border-color: #3b2b5a;");
            validateAll();
        });
        datePicker.valueProperty().addListener((o, old, n) -> {
            boolean invalid = n != null && n.isBefore(LocalDate.now());
            errDate.setVisible(invalid);
            validateAll();
        });
        fieldJoueur1.textProperty().addListener((o, old, n) -> {
            boolean invalid = n.length() < 3;
            errJoueur1.setVisible(invalid);
            fieldJoueur1.setStyle(invalid ? "-fx-border-color: #ff4757;" : "-fx-border-color: #3b2b5a;");
            validateAll();
        });
        fieldJoueur2.textProperty().addListener((o, old, n) -> {
            boolean invalid = n.length() < 3;
            errJoueur2.setVisible(invalid);
            fieldJoueur2.setStyle(invalid ? "-fx-border-color: #ff4757;" : "-fx-border-color: #3b2b5a;");
            validateAll();
        });
        fieldScore1.textProperty().addListener((o, old, n) -> validateScores());
        fieldScore2.textProperty().addListener((o, old, n) -> validateScores());
    }

    private void validateScores() {
        boolean invalid = !fieldScore1.getText().matches("\\d*") || !fieldScore2.getText().matches("\\d*");
        errScores.setVisible(invalid);
        validateAll();
    }

    private void validateAll() {
        boolean isValid = fieldRound.getText().length() >= 3 &&
                          (datePicker.getValue() == null || !datePicker.getValue().isBefore(LocalDate.now())) &&
                          fieldJoueur1.getText().length() >= 3 &&
                          fieldJoueur2.getText().length() >= 3 &&
                          fieldScore1.getText().matches("\\d+") &&
                          fieldScore2.getText().matches("\\d+") &&
                          comboTournoi.getValue() != null;
        btnSave.setDisable(!isValid);
    }

    private void loadTournaments() {
        List<Tournament> tournaments = tournamentService.findAll();
        comboTournoi.setItems(FXCollections.observableArrayList(tournaments));
        comboTournoi.setConverter(new StringConverter<>() {
            @Override public String toString(Tournament t) { return (t == null) ? "" : "[" + t.getId() + "] " + t.getNom(); }
            @Override public Tournament fromString(String s) { return null; }
        });
        comboTournoi.valueProperty().addListener((o, old, n) -> validateAll());
    }

    public void setMatch(Match m) {
        this.currentMatch = m;
        if (m != null) {
            lblTitle.setText("MODIFIER LE MATCH #" + m.getId());
            for (Tournament t : comboTournoi.getItems()) {
                if (t.getId() == m.getTournoiId()) { comboTournoi.setValue(t); break; }
            }
            fieldRound.setText(m.getRound());
            datePicker.setValue(m.getDateMatch().toLocalDate());
            fieldTime.setText(m.getDateMatch().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            comboStatut.setValue(m.getStatut());
            fieldJoueur1.setText(m.getNomJoueur1());
            fieldJoueur2.setText(m.getNomJoueur2());
            fieldScore1.setText(String.valueOf(m.getScoreJoueur1()));
            fieldScore2.setText(String.valueOf(m.getScoreJoueur2()));
        } else {
            lblTitle.setText("NOUVEAU MATCH");
        }
        validateAll();
    }

    @FXML
    private void onSave() {
        Tournament selectedT = comboTournoi.getValue();
        LocalTime time = LocalTime.parse(fieldTime.getText());
        LocalDateTime dateTime = LocalDateTime.of(datePicker.getValue(), time);
        if (currentMatch == null) {
            currentMatch = new Match(selectedT.getId(), dateTime, fieldRound.getText(), comboStatut.getValue(),
                    fieldJoueur1.getText(), fieldJoueur2.getText(),
                    Integer.parseInt(fieldScore1.getText()), Integer.parseInt(fieldScore2.getText()));
            if (matchService.save(currentMatch)) { saved = true; close(); }
        } else {
            currentMatch.setTournoiId(selectedT.getId());
            currentMatch.setDateMatch(dateTime);
            currentMatch.setRound(fieldRound.getText());
            currentMatch.setStatut(comboStatut.getValue());
            currentMatch.setNomJoueur1(fieldJoueur1.getText());
            currentMatch.setNomJoueur2(fieldJoueur2.getText());
            currentMatch.setScoreJoueur1(Integer.parseInt(fieldScore1.getText()));
            currentMatch.setScoreJoueur2(Integer.parseInt(fieldScore2.getText()));
            if (matchService.update(currentMatch)) { saved = true; close(); }
        }
    }

    @FXML private void onCancel() { close(); }
    private void close() { ((Stage) fieldRound.getScene().getWindow()).close(); }
    public boolean isSaved() { return saved; }
}
