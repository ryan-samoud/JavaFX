package com.esports.controller;

import com.esports.interfaces.IEvenementService;
import com.esports.model.Evenement;
import com.esports.service.EvenementService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EvenementsPublicController implements Initializable {

    @FXML private Label            lblTotalCount;
    @FXML private Label            lblUpcomingCount;
    @FXML private Label            lblPassedCount;
    @FXML private TextField        fieldSearch;
    @FXML private ComboBox<String> comboFilter;
    @FXML private ComboBox<String> comboSort;
    @FXML private FlowPane         paneUpcoming;
    @FXML private FlowPane         panePassed;
    @FXML private Label            lblEmpty;
    @FXML private Label            lblCalendarMonth;
    @FXML private GridPane         calendarGrid;

    private final IEvenementService dao = new EvenementService();
    private List<Evenement> allEvents   = new ArrayList<>();
    private final Set<Integer> participatedEvents = new HashSet<>();
    private YearMonth currentMonth = YearMonth.now();

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboFilter.getItems().addAll("Tous", "À venir", "Passés");
        comboFilter.setValue("Tous");
        comboSort.getItems().addAll("Date (récent)", "Date (ancien)", "Nom A→Z", "Participants ↓");
        comboSort.setValue("Date (récent)");
        fieldSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        comboFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        comboSort.valueProperty().addListener((obs, o, n)   -> applyFilter());
        loadEvents();
        renderCalendar();
    }

    private void loadEvents() {
        try { allEvents = dao.findAll(); }
        catch (Exception e) { System.err.println("[Public] " + e.getMessage()); allEvents = new ArrayList<>(); }
        applyFilter();
        renderCalendar();
    }

    private void participer(Evenement e) {
        dao.incrementParticipants(e.getId());
        participatedEvents.add(e.getId());
        loadEvents();
    }

    private void annulerParticipation(Evenement e) {
        dao.decrementParticipants(e.getId());
        participatedEvents.remove(e.getId());
        loadEvents();
    }

    private void openDetails(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esports/fxml/EventDetails.fxml"));
            Parent root = loader.load();
            EventDetailsController ctrl = loader.getController();
            ctrl.setEvent(e);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 700, 750));
            stage.setTitle("Détails — " + e.getNom());
            stage.show();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML private void onSearch() { applyFilter(); }

    private void applyFilter() {
        String query  = fieldSearch.getText().trim().toLowerCase();
        String filter = comboFilter.getValue();
        String sort   = comboSort.getValue();

        List<Evenement> filtered = allEvents.stream()
                .filter(e -> query.isEmpty()
                        || e.getNom().toLowerCase().contains(query)
                        || (e.getLieu() != null && e.getLieu().toLowerCase().contains(query)))
                .filter(e -> {
                    if ("À venir".equals(filter)) return !e.isPast();
                    if ("Passés".equals(filter))  return  e.isPast();
                    return true;
                }).collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Date (récent)"  -> filtered.sort((a, b) -> b.getDate() == null ? 1 : a.getDate() == null ? -1 : b.getDate().compareTo(a.getDate()));
            case "Date (ancien)"  -> filtered.sort((a, b) -> a.getDate() == null ? 1 : b.getDate() == null ? -1 : a.getDate().compareTo(b.getDate()));
            case "Nom A→Z"        -> filtered.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Participants ↓" -> filtered.sort((a, b) -> Integer.compare(b.getNbrParticipant(), a.getNbrParticipant()));
        }

        updateStats(filtered);
        renderCards(filtered);
    }

    private void updateStats(List<Evenement> list) {
        lblTotalCount.setText(String.valueOf(list.size()));
        lblUpcomingCount.setText(String.valueOf(list.stream().filter(e -> !e.isPast()).count()));
        lblPassedCount.setText(String.valueOf(list.stream().filter(Evenement::isPast).count()));
    }

    @FXML private void onPrevMonth() { currentMonth = currentMonth.minusMonths(1); renderCalendar(); }
    @FXML private void onNextMonth() { currentMonth = currentMonth.plusMonths(1);  renderCalendar(); }

    private void renderCalendar() {
        if (calendarGrid == null || lblCalendarMonth == null) return;
        lblCalendarMonth.setText(currentMonth.format(MONTH_FMT).toUpperCase());
        calendarGrid.getChildren().clear();
        calendarGrid.setHgap(4); calendarGrid.setVgap(4);

        Set<Integer> eventDays = allEvents.stream()
                .filter(e -> e.getDate() != null
                        && e.getDate().getYear() == currentMonth.getYear()
                        && e.getDate().getMonthValue() == currentMonth.getMonthValue())
                .map(e -> e.getDate().getDayOfMonth()).collect(Collectors.toSet());

        String[] days = {"L","M","M","J","V","S","D"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(days[i]);
            h.setStyle("-fx-text-fill:#7c3aed;-fx-font-size:11px;-fx-font-weight:bold;-fx-font-family:'Courier New';-fx-min-width:28px;");
            h.setMaxWidth(Double.MAX_VALUE); h.setAlignment(Pos.CENTER);
            calendarGrid.add(h, i, 0);
        }

        int startCol = currentMonth.atDay(1).getDayOfWeek().getValue() - 1;
        LocalDate today = LocalDate.now();
        int col = startCol, row = 1;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            Label lbl = new Label(String.valueOf(day));
            lbl.setMaxWidth(Double.MAX_VALUE); lbl.setAlignment(Pos.CENTER);
            LocalDate thisDay = currentMonth.atDay(day);
            boolean isToday = thisDay.equals(today), hasEvent = eventDays.contains(day);

            String style = "-fx-font-size:11px;-fx-font-family:'Courier New';-fx-min-width:28px;-fx-min-height:26px;-fx-background-radius:6px;";
            if (isToday)       style += "-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-font-weight:bold;";
            else if (hasEvent) {
                style += "-fx-background-color:rgba(168,85,247,0.2);-fx-text-fill:#c084fc;-fx-font-weight:bold;";
                List<String> names = allEvents.stream()
                        .filter(e -> e.getDate() != null && e.getDate().equals(thisDay))
                        .map(Evenement::getNom).collect(Collectors.toList());
                Tooltip tip = new Tooltip(String.join("\n", names));
                tip.setStyle("-fx-background-color:#1a1035;-fx-text-fill:#c084fc;-fx-font-family:'Courier New';");
                Tooltip.install(lbl, tip);
                lbl.setCursor(javafx.scene.Cursor.HAND);
            } else style += "-fx-text-fill:#6b7280;";

            lbl.setStyle(style);
            calendarGrid.add(lbl, col, row);
            col++; if (col == 7) { col = 0; row++; }
        }
    }

    private void renderCards(List<Evenement> list) {
        paneUpcoming.getChildren().clear();
        panePassed.getChildren().clear();
        if (list.isEmpty()) { lblEmpty.setVisible(true); lblEmpty.setManaged(true); return; }
        lblEmpty.setVisible(false); lblEmpty.setManaged(false);
        for (Evenement e : list) {
            VBox card = buildCard(e);
            if (e.isPast()) panePassed.getChildren().add(card);
            else            paneUpcoming.getChildren().add(card);
        }
    }

    private VBox buildCard(Evenement e) {
        String accent = e.isPast() ? "#4b5563" : "#a855f7";
        String border = e.isPast() ? "rgba(75,85,99,0.25)" : "rgba(168,85,247,0.25)";

        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setStyle(cardStyle(border, false));
        card.setOnMouseEntered(ev -> card.setStyle(cardStyle(accent, true)));
        card.setOnMouseExited(ev  -> card.setStyle(cardStyle(border, false)));

        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                ImageView iv = new ImageView(new Image(
                        new File("src/main/resources/images/events/" + e.getImage()).toURI().toString()));
                iv.setFitWidth(280); iv.setFitHeight(130); iv.setPreserveRatio(false);
                card.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 18, 16, 18));

        HBox topRow = new HBox(8); topRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(e.isPast() ? "✓ PASSÉ" : "◆ À VENIR");
        badge.setStyle("-fx-text-fill:" + accent + ";-fx-background-color:" +
                (e.isPast() ? "rgba(75,85,99,0.15)" : "rgba(168,85,247,0.12)") + ";" +
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:4 10 4 10;-fx-background-radius:20px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label partLabel = new Label("👥 " + e.getNbrParticipant());
        partLabel.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:12px;");
        topRow.getChildren().addAll(badge, spacer, partLabel);

        Label nom = new Label(e.getNom());
        nom.setStyle("-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:bold;");
        nom.setWrapText(true); nom.setMaxWidth(240);

        Label lieu = new Label("📍 " + (e.getLieu() != null ? e.getLieu() : "—"));
        lieu.setStyle("-fx-text-fill:" + accent + ";-fx-font-size:12px;-fx-font-weight:bold;");
        lieu.setWrapText(true); lieu.setMaxWidth(240);

        Label date = new Label("📅 " + (e.getDate() != null ? e.getDate().format(DATE_FMT) : "—"));
        date.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:12px;");

        String descText = e.getDescription() != null ? e.getDescription() : "";
        if (descText.length() > 70) descText = descText.substring(0, 70) + "…";
        Label desc = new Label(descText);
        desc.setStyle("-fx-text-fill:#6b7280;-fx-font-size:11px;");
        desc.setWrapText(true); desc.setMaxWidth(240);

        Region bot = new Region(); VBox.setVgrow(bot, Priority.ALWAYS);
        content.getChildren().addAll(topRow, nom, lieu, date, desc, bot);

        // ── Buttons ─────────────────────────────────────────
        Button btnDetails = new Button("Voir les détails");
        btnDetails.setStyle(outlineBtn());
        btnDetails.setOnAction(ev -> openDetails(e));

        if (e.isPast()) {
            btnDetails.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().add(btnDetails);
        } else {
            // Two buttons side by side for upcoming events
            boolean isParticipating = participatedEvents.contains(e.getId());
            Button btnPart = isParticipating ? new Button("Annuler") : new Button("Participer →");
            if (isParticipating) {
                btnPart.setStyle("-fx-background-color:rgba(239,68,68,0.15);-fx-text-fill:#f87171;" +
                        "-fx-border-color:rgba(239,68,68,0.4);-fx-border-width:1px;" +
                        "-fx-border-radius:8px;-fx-background-radius:8px;" +
                        "-fx-font-size:12px;-fx-padding:8 0 8 0;-fx-cursor:hand;");
                btnPart.setOnAction(ev -> annulerParticipation(e));
            } else {
                btnPart.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#ec4899);" +
                        "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-padding:8 0 8 0;-fx-border-radius:8px;-fx-background-radius:8px;-fx-cursor:hand;");
                btnPart.setOnAction(ev -> participer(e));
            }
            HBox.setHgrow(btnDetails, Priority.ALWAYS); btnDetails.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnPart,    Priority.ALWAYS); btnPart.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().add(new HBox(8, btnDetails, btnPart));
        }

        card.getChildren().add(content);
        return card;
    }

    private String outlineBtn() {
        return "-fx-background-color:transparent;-fx-text-fill:#9ca3af;" +
                "-fx-border-color:#374151;-fx-border-width:1px;-fx-border-radius:8px;" +
                "-fx-background-radius:8px;-fx-font-size:12px;-fx-padding:8 0 8 0;-fx-cursor:hand;";
    }

    private String cardStyle(String borderColor, boolean hovered) {
        String s = "-fx-background-color:" + (hovered ? "rgba(26,14,55,0.98)" : "rgba(17,11,40,0.92)") + ";" +
                "-fx-border-color:" + borderColor + ";-fx-border-width:1.5px;" +
                "-fx-border-radius:14px;-fx-background-radius:14px;-fx-cursor:hand;";
        if (hovered) s += "-fx-effect:dropshadow(gaussian," + borderColor + ",18,0.3,0,4);";
        return s;
    }

    @FXML
    private void onBackHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/esports/fxml/HomeView.fxml"));
            Stage stage = (Stage) fieldSearch.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setWidth(w); stage.setHeight(h);
            stage.setTitle("NexUS Gaming Arena");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
