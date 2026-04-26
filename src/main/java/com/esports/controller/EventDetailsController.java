package com.esports.controller;

import com.esports.interfaces.ICommentaireService;
import com.esports.model.Commentaire;
import com.esports.model.Evenement;
import com.esports.model.User;
import com.esports.service.AuthService;
import com.esports.service.CommentaireService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class EventDetailsController {

    @FXML private Label    lblNom;
    @FXML private Label    lblLieu;
    @FXML private Label    lblDate;
    @FXML private Label    lblDesc;
    @FXML private Label    lblParticipants;
    @FXML private ImageView imageView;
    @FXML private WebView  mapView;
    @FXML private VBox     paneComments;
    @FXML private TextArea fieldCommentaire;
    @FXML private Label    lblCommentError;
    @FXML private VBox     vboxCommentForm;

    private final ICommentaireService commentaireService = new CommentaireService();
    private Evenement currentEvent;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Bad words list — extend as needed
    private static final Set<String> BAD_WORDS = Set.of(
            "idiot", "stupide", "nul", "merde", "putain", "connard", "imbecile",
            "crétin", "salaud", "enculé", "con", "batard", "pd", "fdp",
            "shit", "fuck", "asshole", "bastard", "bitch", "crap", "damn"
    );

    // ── Setup ─────────────────────────────────────────────────────

    public void setEvent(Evenement e) {
        this.currentEvent = e;

        lblNom.setText(e.getNom());
        lblLieu.setText("📍 " + (e.getLieu() != null ? e.getLieu() : "—"));
        lblDate.setText("📅 " + (e.getDate() != null ? e.getDate().format(DATE_FMT) : "—"));
        lblDesc.setText(e.getDescription() != null ? e.getDescription() : "");
        lblParticipants.setText("👥 " + e.getNbrParticipant() + " participants");

        if (e.getImage() != null && !e.getImage().isEmpty()) {
            try {
                imageView.setImage(new Image(
                        new File("src/main/resources/images/events/" + e.getImage()).toURI().toString()));
            } catch (Exception ignored) {}
        }

        Platform.runLater(() -> loadMap(e.getLieu()));
        loadComments();

        // Show/hide form based on login
        boolean loggedIn = AuthService.isLoggedIn();
        if (vboxCommentForm != null) {
            vboxCommentForm.setVisible(loggedIn);
            vboxCommentForm.setManaged(loggedIn);
        }
    }

    // ── Map ───────────────────────────────────────────────────────

    private void loadMap(String lieu) {
        if (mapView == null || lieu == null || lieu.isEmpty()) return;
        String encoded;
        try { encoded = URLEncoder.encode(lieu, StandardCharsets.UTF_8); }
        catch (Exception e) { encoded = lieu; }

        String html = "<!DOCTYPE html><html><head>" +
                "<meta charset='utf-8'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>*{margin:0;padding:0;}html,body,#map{width:100%;height:100%;}" +
                "body{background:#0d0b1e;}" +
                "#error{display:none;position:absolute;top:10px;left:50%;transform:translateX(-50%);" +
                "background:rgba(0,0,0,0.8);color:#f87171;padding:8px 16px;" +
                "border-radius:8px;font-size:12px;z-index:999;}" +
                "</style></head><body>" +
                "<div id='map'></div><div id='error'>Lieu introuvable sur la carte</div>" +
                "<script>" +
                "var map=L.map('map',{zoomControl:true,attributionControl:false});" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18}).addTo(map);" +
                "fetch('https://nominatim.openstreetmap.org/search?q=" + encoded +
                "&format=json&limit=1',{headers:{'Accept-Language':'fr'}})" +
                ".then(r=>r.json()).then(data=>{" +
                "  if(data&&data.length>0){" +
                "    var lat=parseFloat(data[0].lat),lon=parseFloat(data[0].lon);" +
                "    map.setView([lat,lon],14);" +
                "    L.marker([lat,lon]).addTo(map)" +
                "     .bindPopup('<b>" + lieu.replace("'", "\\'") + "</b>').openPopup();" +
                "  }else{map.setView([36.8065,10.1815],10);" +
                "    document.getElementById('error').style.display='block';}" +
                "}).catch(()=>map.setView([36.8065,10.1815],10));" +
                "</script></body></html>";

        mapView.getEngine().loadContent(html);
    }

    // ── Comments ──────────────────────────────────────────────────

    private void loadComments() {
        if (paneComments == null || currentEvent == null) return;
        paneComments.getChildren().clear();

        List<Commentaire> comments = commentaireService.findByEvenement(currentEvent.getId());
        if (comments.isEmpty()) {
            Label empty = new Label("Aucun commentaire. Soyez le premier !");
            empty.setStyle("-fx-text-fill:#6b7280;-fx-font-size:13px;-fx-font-family:'Courier New';");
            paneComments.getChildren().add(empty);
            return;
        }

        User currentUser = AuthService.getCurrentUser();
        boolean isAdmin  = currentUser != null && currentUser.isAdmin();

        for (Commentaire c : comments) {
            paneComments.getChildren().add(buildCommentCard(c, currentUser, isAdmin));
        }
    }

    private VBox buildCommentCard(Commentaire c, User currentUser, boolean isAdmin) {
        boolean isOwn = currentUser != null && c.getUserId() == currentUser.getId();
        boolean flagged = c.isFlagged();

        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        String borderColor = flagged ? "rgba(239,68,68,0.35)" : "rgba(139,92,246,0.2)";
        String bgColor = flagged ? "rgba(239,68,68,0.06)" : "rgba(26,14,55,0.6)";
        card.setStyle("-fx-background-color:" + bgColor + ";" +
                "-fx-border-color:" + borderColor + ";-fx-border-width:1;" +
                "-fx-border-radius:10px;-fx-background-radius:10px;");

        // Header
        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        String auteurText = c.getAuteurNom() != null && !c.getAuteurNom().isBlank()
                ? c.getAuteurNom() : "Utilisateur";
        Label auteur = new Label("👤 " + auteurText);
        auteur.setStyle("-fx-text-fill:#c084fc;-fx-font-weight:bold;-fx-font-size:13px;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(TIME_FMT) : "");
        dateLabel.setStyle("-fx-text-fill:#6b7280;-fx-font-size:11px;");

        header.getChildren().addAll(auteur, spacer, dateLabel);

        // Flagged badge
        if (flagged) {
            Label flagBadge = new Label("🚩 En cours de révision");
            flagBadge.setStyle("-fx-text-fill:#ef4444;-fx-background-color:rgba(239,68,68,0.12);" +
                    "-fx-border-color:rgba(239,68,68,0.3);-fx-border-width:1;" +
                    "-fx-border-radius:20px;-fx-background-radius:20px;" +
                    "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3 10 3 10;");
            header.getChildren().add(flagBadge);
        }

        // Content label
        Label contenu = new Label(c.getContenu());
        contenu.setStyle("-fx-text-fill:#d1d5db;-fx-font-size:13px;");
        contenu.setWrapText(true);

        // Edit/Delete for own comments or admin
        if (isOwn || isAdmin) {
            Button btnEdit   = smallBtn("✏", "#60a5fa");
            Button btnDelete = smallBtn("🗑", "#f87171");
            header.getChildren().addAll(btnEdit, btnDelete);

            // Inline edit area
            TextArea editArea = new TextArea(c.getContenu());
            editArea.setStyle("-fx-background-color:#1a1035;-fx-text-fill:#e2e8f0;" +
                    "-fx-border-color:rgba(139,92,246,0.3);-fx-border-width:1;" +
                    "-fx-border-radius:6;-fx-background-radius:6;" +
                    "-fx-font-family:'Courier New';-fx-font-size:12px;" +
                    "-fx-control-inner-background:#1a1035;");
            editArea.setPrefRowCount(2);
            editArea.setVisible(false); editArea.setManaged(false);

            Button btnSave   = new Button("Enregistrer");
            Button btnCancel = new Button("Annuler");
            btnSave.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#ec4899);" +
                    "-fx-text-fill:white;-fx-font-size:12px;-fx-padding:5 16 5 16;" +
                    "-fx-background-radius:6px;-fx-cursor:hand;");
            btnCancel.setStyle("-fx-background-color:transparent;-fx-text-fill:#9ca3af;" +
                    "-fx-border-color:rgba(139,92,246,0.3);-fx-border-width:1px;" +
                    "-fx-border-radius:6px;-fx-background-radius:6px;" +
                    "-fx-font-size:12px;-fx-padding:5 16 5 16;-fx-cursor:hand;");
            HBox editBtns = new HBox(8, btnCancel, btnSave);
            editBtns.setVisible(false); editBtns.setManaged(false);

            btnEdit.setOnAction(ev -> {
                boolean show = !editArea.isVisible();
                editArea.setVisible(show); editArea.setManaged(show);
                editBtns.setVisible(show); editBtns.setManaged(show);
                contenu.setVisible(!show); contenu.setManaged(!show);
            });
            btnSave.setOnAction(ev -> {
                String txt = editArea.getText().trim();
                if (txt.isEmpty()) return;
                c.setContenu(txt);
                if (commentaireService.update(c)) {
                    contenu.setText(txt);
                    editArea.setVisible(false); editArea.setManaged(false);
                    editBtns.setVisible(false); editBtns.setManaged(false);
                    contenu.setVisible(true); contenu.setManaged(true);
                }
            });
            btnCancel.setOnAction(ev -> {
                editArea.setVisible(false); editArea.setManaged(false);
                editBtns.setVisible(false); editBtns.setManaged(false);
                contenu.setVisible(true); contenu.setManaged(true);
            });
            btnDelete.setOnAction(ev -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES && commentaireService.delete(c.getId()))
                        loadComments();
                });
            });

            card.getChildren().addAll(header, contenu, editArea, editBtns);
        } else {
            card.getChildren().addAll(header, contenu);
        }

        return card;
    }

    // ── Add comment ───────────────────────────────────────────────

    @FXML
    private void onAddComment() {
        if (currentEvent == null) return;
        if (!AuthService.isLoggedIn()) {
            lblCommentError.setText("Vous devez être connecté pour commenter.");
            return;
        }

        String contenu = fieldCommentaire.getText().trim();
        if (contenu.isEmpty())   { lblCommentError.setText("Le commentaire ne peut pas être vide."); return; }
        if (contenu.length() < 3){ lblCommentError.setText("Le commentaire doit contenir au moins 3 caractères."); return; }

        lblCommentError.setText("");

        boolean hasBadWord = containsBadWord(contenu);
        User user = AuthService.getCurrentUser();
        Commentaire c = new Commentaire(contenu, currentEvent.getId(), user.getId());
        c.setFlagged(hasBadWord);

        if (commentaireService.save(c)) {
            fieldCommentaire.clear();
            if (hasBadWord) {
                // Show warning but still post
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Commentaire signalé");
                alert.setHeaderText(null);
                alert.setContentText("Votre commentaire contient des mots inappropriés et sera examiné par un administrateur avant d'être publié.");
                alert.showAndWait();
            }
            loadComments();
        } else {
            lblCommentError.setText("Erreur lors de l'envoi du commentaire.");
        }
    }

    private boolean containsBadWord(String text) {
        String lower = text.toLowerCase();
        for (String word : BAD_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private Button smallBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "44;" +
                "-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;" +
                "-fx-padding:2 7 2 7;-fx-cursor:hand;-fx-font-size:11px;");
        return b;
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }
}