package com.esports.controller;

import com.esports.model.Commande;
import com.esports.model.PanierItem;
import com.esports.service.AuthService;
import com.esports.service.CommandeService;
import com.esports.service.PanierService;
import com.esports.service.PanierSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class PaiementController implements Initializable {

    @FXML private TextField fieldCarte;
    @FXML private TextField fieldExpiration;
    @FXML private TextField fieldCvv;
    @FXML private TextField fieldNomCarte;
    @FXML private TextField fieldAdresse;
    @FXML private TextField fieldTelephone;
    @FXML private TextArea  fieldNotes;
    @FXML private Label     lblErreur;
    @FXML private Label     lblTotal;
    @FXML private VBox      vboxArticles;
    @FXML private Button    btnPayer;

    private final PanierService   panierService   = new PanierService();
    private final CommandeService commandeService = new CommandeService();

    private List<PanierItem> items;
    private double total;
    private int panierId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        panierId = PanierSession.getPanierId();
        items = panierService.getItems(panierId);
        total = panierService.getTotal(panierId);

        lblTotal.setText(String.format("%.0f DT", total));
        afficherArticles();
        setupFormatCarte();
    }

    private void afficherArticles() {
        vboxArticles.getChildren().clear();
        for (PanierItem item : items) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            Label lblNom = new Label(item.getNomProduit() + " x" + item.getQuantite());
            lblNom.setStyle("-fx-text-fill: #c4b5fd; -fx-font-size: 13px;");
            lblNom.setMaxWidth(160);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label lblPrix = new Label(String.format("%.0f DT", item.getTotalLigne()));
            lblPrix.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            row.getChildren().addAll(lblNom, spacer, lblPrix);
            vboxArticles.getChildren().add(row);
        }
    }

    // ── Format automatique carte : XXXX XXXX XXXX XXXX ──
    private void setupFormatCarte() {
        fieldCarte.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }
            if (!formatted.toString().equals(newVal)) {
                fieldCarte.setText(formatted.toString());
                fieldCarte.positionCaret(formatted.length());
            }
        });

        fieldExpiration.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            String formatted = digits.length() > 2
                    ? digits.substring(0, 2) + "/" + digits.substring(2)
                    : digits;
            if (!formatted.equals(newVal)) {
                fieldExpiration.setText(formatted);
                fieldExpiration.positionCaret(formatted.length());
            }
        });

        fieldCvv.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 3) digits = digits.substring(0, 3);
            if (!digits.equals(newVal)) {
                fieldCvv.setText(digits);
                fieldCvv.positionCaret(digits.length());
            }
        });
    }

    @FXML
    private void onPayer() {
        // Validation
        if (!valider()) return;

        // Simulation paiement
        btnPayer.setText("⏳ Traitement en cours...");
        btnPayer.setDisable(true);

        // Délai simulé 2 secondes
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> {
            // Validation Luhn
            String numeroSansEspaces = fieldCarte.getText().replace(" ", "");
            if (!validerLuhn(numeroSansEspaces)) {
                afficherErreur("❌ Numéro de carte invalide.");
                btnPayer.setText("🔒  Payer maintenant");
                btnPayer.setDisable(false);
                return;
            }

            // Créer commande
            int userId = AuthService.isLoggedIn() ? AuthService.getCurrentUser().getId() : 0;
            String modePaiement = "Carte bancaire **** " +
                    numeroSansEspaces.substring(numeroSansEspaces.length() - 4);

            Commande commande = commandeService.creerCommande(
                    items, total,
                    fieldAdresse.getText().trim(),
                    fieldTelephone.getText().trim(),
                    fieldNotes.getText().trim(),
                    modePaiement, userId
            );

            if (commande != null) {
                // Vider le panier
                panierService.viderPanier(panierId);
                // Naviguer vers la page succès
                ouvrirSucces(commande);
            } else {
                afficherErreur("❌ Erreur lors de la création de la commande.");
                btnPayer.setText("🔒  Payer maintenant");
                btnPayer.setDisable(false);
            }
        });
        pause.play();
    }

    private boolean valider() {
        String carte = fieldCarte.getText().replace(" ", "");
        String exp = fieldExpiration.getText().trim();
        String cvv = fieldCvv.getText().trim();
        String nom = fieldNomCarte.getText().trim();
        String adresse = fieldAdresse.getText().trim();
        String tel = fieldTelephone.getText().trim();

        if (carte.length() != 16) {
            afficherErreur("❌ Le numéro de carte doit contenir 16 chiffres.");
            return false;
        }
        if (!exp.matches("\\d{2}/\\d{2}")) {
            afficherErreur("❌ Date d'expiration invalide (format MM/AA).");
            return false;
        }
        if (cvv.length() != 3) {
            afficherErreur("❌ Le CVV doit contenir 3 chiffres.");
            return false;
        }
        if (nom.isEmpty()) {
            afficherErreur("❌ Veuillez saisir le nom sur la carte.");
            return false;
        }
        if (adresse.isEmpty()) {
            afficherErreur("❌ Veuillez saisir votre adresse de livraison.");
            return false;
        }
        if (tel.isEmpty()) {
            afficherErreur("❌ Veuillez saisir votre numéro de téléphone.");
            return false;
        }
        if (items.isEmpty()) {
            afficherErreur("❌ Votre panier est vide.");
            return false;
        }
        lblErreur.setVisible(false);
        lblErreur.setManaged(false);
        return true;
    }

    // ── Algorithme de Luhn ──
    private boolean validerLuhn(String numero) {
        int sum = 0;
        boolean alternate = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(String.valueOf(numero.charAt(i)));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void afficherErreur(String msg) {
        lblErreur.setText(msg);
        lblErreur.setVisible(true);
        lblErreur.setManaged(true);
    }

    private void ouvrirSucces(Commande commande) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esports/fxml/CommandeSuccesView.fxml"));
            Parent root = loader.load();
            CommandeSuccesController ctrl = loader.getController();
            ctrl.setCommande(commande);

            Stage stage = (Stage) btnPayer.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("Commande confirmée");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackPanier() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/esports/fxml/PanierView.fxml"));
            Stage stage = (Stage) btnPayer.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
            stage.setTitle("Mon Panier");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}