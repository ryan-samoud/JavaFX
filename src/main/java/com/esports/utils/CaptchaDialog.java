package com.esports.utils;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.*;

/**
 * Sliding-puzzle CAPTCHA dialog (Genshin Impact style).
 *
 * Usage:
 *   boolean ok = CaptchaDialog.show(parentWindow);
 *
 * Images: place numbered files (1.jpg, 2.png, …) in
 *   src/main/resources/com/esports/captcha/
 * If none are found the dialog falls back to a generated image.
 */
public class CaptchaDialog {

    // ── Dimensions ────────────────────────────────────────────────
    private static final int    IMG_W     = 320;
    private static final int    IMG_H     = 160;
    private static final int    PIECE_W   = 60;
    private static final int    PIECE_H   = 60;
    private static final int    PIECE_Y   = (IMG_H - PIECE_H) / 2;   // 50 px from top
    private static final int    TOLERANCE = 10;                        // ± pixels allowed
    private static final double HANDLE_W  = 48;
    private static final double TRACK_H   = 44;

    // ── State ─────────────────────────────────────────────────────
    private final Random  random   = new Random();
    private int           targetX;          // correct piece X position
    private double        dragX    = 0;     // current piece X position
    private int           attempts = 0;
    private boolean       result   = false;
    private boolean       locked   = false; // true while success animation plays

    // ── UI refs ───────────────────────────────────────────────────
    private Stage         stage;
    private Canvas        canvas;
    private Label         lblStatus;
    private Pane          handlePane;
    private Image         sourceImage;
    private WritableImage pieceImage;

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /** Show the dialog and return {@code true} if the user solves the puzzle. */
    public static boolean show(Window owner) {
        return new CaptchaDialog().showDialog(owner);
    }

    // ─────────────────────────────────────────────────────────────
    // DIALOG BOOTSTRAP
    // ─────────────────────────────────────────────────────────────

    private boolean showDialog(Window owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);

        VBox root = buildUI();
        root.setStyle(
            "-fx-background-color:#1a1535;" +
            "-fx-border-color:rgba(168,85,247,0.55);" +
            "-fx-border-width:1.5;-fx-border-radius:14;-fx-background-radius:14;" +
            "-fx-effect:dropshadow(gaussian,rgba(168,85,247,0.45),32,0.3,0,0);"
        );

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        generatePuzzle();
        stage.centerOnScreen();
        stage.showAndWait();
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // UI CONSTRUCTION
    // ─────────────────────────────────────────────────────────────

    private VBox buildUI() {
        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
            "-fx-background-color:linear-gradient(to right,#4c1d95,#7c3aed);" +
            "-fx-background-radius:14 14 0 0;"
        );
        Label ico = new Label("🔒");
        ico.setStyle("-fx-font-size:15px;");
        Label ttl = new Label("VÉRIFICATION DE SÉCURITÉ");
        ttl.setStyle(
            "-fx-text-fill:white;-fx-font-size:12px;" +
            "-fx-font-weight:bold;-fx-font-family:'Courier New';"
        );
        header.getChildren().addAll(ico, ttl);

        // ── Canvas (puzzle image) ──────────────────────────────────
        canvas = new Canvas(IMG_W, IMG_H);
        StackPane canvasBox = new StackPane(canvas);
        canvasBox.setStyle("-fx-border-color:rgba(168,85,247,0.4);-fx-border-width:1;");

        // ── Hint ──────────────────────────────────────────────────
        Label hint = new Label("Faites glisser la pièce vers l'emplacement correspondant");
        hint.setStyle("-fx-text-fill:#a78bba;-fx-font-size:12px;");

        // ── Slider track ──────────────────────────────────────────
        Pane track = buildTrack();

        // ── Status label ──────────────────────────────────────────
        lblStatus = new Label(" ");
        lblStatus.setStyle("-fx-text-fill:#a78bba;-fx-font-size:12px;");
        lblStatus.setMinHeight(18);
        lblStatus.setWrapText(true);

        // ── Footer buttons ────────────────────────────────────────
        Button btnRefresh = makeBtn("↺  Nouveau", "#c4b5fd",
                "rgba(168,85,247,0.12)", "rgba(168,85,247,0.35)");
        btnRefresh.setOnAction(e -> { if (!locked) refresh(); });

        Button btnCancel = makeBtn("× Annuler", "#f87171",
                "rgba(239,68,68,0.10)", "rgba(239,68,68,0.30)");
        btnCancel.setOnAction(e -> { result = false; stage.close(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, btnRefresh, spacer, btnCancel);

        // ── Body ──────────────────────────────────────────────────
        VBox body = new VBox(12, canvasBox, hint, track, lblStatus, footer);
        body.setPadding(new Insets(18, 18, 18, 18));
        body.setAlignment(Pos.CENTER_LEFT);

        return new VBox(header, body);
    }

    private Pane buildTrack() {
        Pane track = new Pane();
        track.setPrefSize(IMG_W, TRACK_H);

        // Background
        Region bg = new Region();
        bg.setPrefSize(IMG_W, TRACK_H);
        bg.setStyle(
            "-fx-background-color:rgba(255,255,255,0.05);" +
            "-fx-background-radius:22;" +
            "-fx-border-color:rgba(168,85,247,0.3);" +
            "-fx-border-radius:22;-fx-border-width:1;"
        );

        // Center guide line
        Region line = new Region();
        line.setPrefSize(IMG_W - HANDLE_W - 12, 2);
        line.setLayoutX(HANDLE_W / 2 + 6);
        line.setLayoutY(TRACK_H / 2 - 1);
        line.setStyle("-fx-background-color:rgba(168,85,247,0.25);");

        // Draggable handle
        handlePane = new Pane();
        handlePane.setPrefSize(HANDLE_W, TRACK_H - 4);
        handlePane.setLayoutX(2);
        handlePane.setLayoutY(2);
        handlePane.setStyle("-fx-cursor:hand;");

        Region handleBg = new Region();
        handleBg.setPrefSize(HANDLE_W, TRACK_H - 4);
        handleBg.setStyle(
            "-fx-background-color:#7c3aed;" +
            "-fx-background-radius:18;" +
            "-fx-border-color:rgba(168,85,247,0.8);" +
            "-fx-border-width:1.5;-fx-border-radius:18;"
        );
        handleBg.setEffect(new DropShadow(8, Color.web("rgba(168,85,247,0.6)")));

        Label arrow = new Label("›");
        arrow.setStyle("-fx-text-fill:white;-fx-font-size:22px;-fx-font-weight:bold;");
        arrow.setPrefSize(HANDLE_W, TRACK_H - 4);
        arrow.setAlignment(Pos.CENTER);

        handlePane.getChildren().addAll(handleBg, arrow);
        track.getChildren().addAll(bg, line, handlePane);

        // Sync dragX + canvas whenever handle X changes (drag AND animation)
        handlePane.layoutXProperty().addListener((obs, oldVal, newVal) -> {
            if (!locked && pieceImage != null) {
                double maxX = IMG_W - HANDLE_W - 2;
                dragX = Math.max(0, (newVal.doubleValue() - 2) / (maxX - 2) * (IMG_W - PIECE_W));
                drawPuzzle(false);
            }
        });

        // Mouse drag
        final double[] anchor = {0, 0}; // [handleStartX, mouseStartX]
        handlePane.setOnMousePressed(e -> {
            if (locked) return;
            anchor[0] = handlePane.getLayoutX();
            anchor[1] = e.getSceneX();
        });
        handlePane.setOnMouseDragged(e -> {
            if (locked) return;
            double newX = anchor[0] + (e.getSceneX() - anchor[1]);
            double maxX = IMG_W - HANDLE_W - 2;
            handlePane.setLayoutX(Math.max(2, Math.min(newX, maxX)));
        });
        handlePane.setOnMouseReleased(e -> { if (!locked) verify(); });

        return track;
    }

    private Button makeBtn(String text, String fg, String bg, String border) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-text-fill:" + fg + ";-fx-font-size:12px;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-radius:8;-fx-background-radius:8;" +
            "-fx-padding:7 14 7 14;-fx-cursor:hand;"
        );
        return b;
    }

    // ─────────────────────────────────────────────────────────────
    // PUZZLE GENERATION
    // ─────────────────────────────────────────────────────────────

    private void generatePuzzle() {
        int minX = PIECE_W + 20;          // 80 px from left
        int maxX = IMG_W - PIECE_W - 20;  // 240 px max
        targetX = minX + random.nextInt(maxX - minX);
        dragX   = 0;
        locked  = false;

        if (handlePane != null) handlePane.setLayoutX(2);

        sourceImage = loadImage();
        PixelReader pr = sourceImage.getPixelReader();
        pieceImage = (pr != null)
            ? new WritableImage(pr, targetX, PIECE_Y, PIECE_W, PIECE_H)
            : new WritableImage(PIECE_W, PIECE_H);

        if (canvas != null) drawPuzzle(false);
    }

    private void refresh() {
        attempts = 0;
        lblStatus.setText(" ");
        lblStatus.setStyle("-fx-text-fill:#a78bba;-fx-font-size:12px;");
        generatePuzzle();
    }

    // ─────────────────────────────────────────────────────────────
    // VERIFICATION
    // ─────────────────────────────────────────────────────────────

    private void verify() {
        double diff = Math.abs(dragX - targetX);

        if (diff <= TOLERANCE) {
            // ── SUCCESS ──
            result = true;
            locked = true;
            lblStatus.setText("✔ Vérification réussie !");
            lblStatus.setStyle("-fx-text-fill:#4ade80;-fx-font-size:12px;-fx-font-weight:bold;");
            drawPuzzle(true);
            PauseTransition pause = new PauseTransition(Duration.millis(750));
            pause.setOnFinished(e -> stage.close());
            pause.play();

        } else {
            // ── FAILURE ──
            attempts++;
            int left = Math.max(0, 5 - attempts);
            lblStatus.setText(left > 0
                ? "✗ Incorrect — " + left + " tentative(s) restante(s)."
                : "✗ Trop de tentatives. Cliquez sur ↺ Nouveau.");
            lblStatus.setStyle("-fx-text-fill:#f87171;-fx-font-size:12px;");

            // Spring-back animation
            new Timeline(
                new KeyFrame(Duration.millis(350),
                    new KeyValue(handlePane.layoutXProperty(), 2.0, Interpolator.EASE_OUT))
            ).play();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DRAWING
    // ─────────────────────────────────────────────────────────────

    private void drawPuzzle(boolean success) {
        if (sourceImage == null || pieceImage == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, IMG_W, IMG_H);

        // 1. Full background image
        gc.drawImage(sourceImage, 0, 0, IMG_W, IMG_H);

        // 2. Hole — darkened area at target position
        gc.setFill(Color.web("rgba(0,0,0,0.62)"));
        gc.fillRoundRect(targetX, PIECE_Y, PIECE_W, PIECE_H, 10, 10);
        gc.setStroke(success ? Color.web("#4ade80") : Color.web("rgba(200,200,255,0.5)"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(targetX, PIECE_Y, PIECE_W, PIECE_H, 10, 10);

        // 3. Sliding piece (rounded-rect clip so edges look clean)
        gc.save();
        rrClip(gc, dragX, PIECE_Y, PIECE_W, PIECE_H, 10);
        gc.drawImage(pieceImage, dragX, PIECE_Y, PIECE_W, PIECE_H);
        gc.restore();

        // Piece border
        gc.setStroke(success ? Color.web("#4ade80") : Color.web("rgba(168,85,247,0.9)"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(dragX, PIECE_Y, PIECE_W, PIECE_H, 10, 10);

        // Success glow ring around the hole
        if (success) {
            gc.setStroke(Color.web("rgba(74,222,128,0.35)"));
            gc.setLineWidth(6);
            gc.strokeRoundRect(targetX - 3, PIECE_Y - 3, PIECE_W + 6, PIECE_H + 6, 13, 13);
        }
    }

    /**
     * Clips the current GraphicsContext to a rounded rectangle.
     * Uses arcTo so it works on JavaFX 17+ (roundRect() requires FX 19).
     */
    private void rrClip(GraphicsContext gc, double x, double y, double w, double h, double r) {
        gc.beginPath();
        gc.moveTo(x + r, y);
        gc.lineTo(x + w - r, y);
        gc.arcTo(x + w, y,      x + w, y + r,      r);
        gc.lineTo(x + w, y + h - r);
        gc.arcTo(x + w, y + h,  x + w - r, y + h,  r);
        gc.lineTo(x + r, y + h);
        gc.arcTo(x,      y + h,  x,      y + h - r, r);
        gc.lineTo(x, y + r);
        gc.arcTo(x,      y,      x + r,  y,          r);
        gc.closePath();
        gc.clip();
    }

    // ─────────────────────────────────────────────────────────────
    // IMAGE LOADING
    // ─────────────────────────────────────────────────────────────

    /**
     * Tries to load a random image from /com/esports/captcha/ (numbered 1–20,
     * .jpg/.jpeg/.png).  Falls back to a generated purple-teal image.
     */
    private Image loadImage() {
        List<String> candidates = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
                candidates.add("/com/esports/captcha/" + i + ext);
            }
        }
        Collections.shuffle(candidates, random);

        for (String path : candidates) {
            try {
                var stream = getClass().getResourceAsStream(path);
                if (stream == null) continue;
                Image img = new Image(stream, IMG_W, IMG_H, false, true);
                if (!img.isError()) return img;
            } catch (Exception ignored) {}
        }
        return generateProceduralImage();
    }

    /** Generates a colourful purple/teal gradient image as fallback. */
    private Image generateProceduralImage() {
        WritableImage img = new WritableImage(IMG_W, IMG_H);
        PixelWriter pw  = img.getPixelWriter();
        int seed = random.nextInt(600);

        for (int y = 0; y < IMG_H; y++) {
            for (int x = 0; x < IMG_W; x++) {
                double nx = x / (double) IMG_W;
                double ny = y / (double) IMG_H;
                double v1 = Math.sin((x + seed) * 0.09) * Math.cos((y + seed) * 0.13) * 0.15;
                double v2 = Math.sin(x * 0.04 + y * 0.07 + seed * 0.01) * 0.10;
                double r  = clamp(0.05 + 0.30 * nx + v1 + 0.12 * Math.sin(ny * Math.PI * 2  + seed * 0.05));
                double g  = clamp(0.02 + 0.07 * ny + v2 + 0.08 * Math.cos(nx * Math.PI * 3));
                double b  = clamp(0.13 + 0.32 * (1 - ny) + v1 + 0.20 * Math.sin((nx + ny) * Math.PI + seed * 0.04));
                pw.setColor(x, y, Color.color(r, g, b));
            }
        }
        return img;
    }

    private double clamp(double v) { return Math.min(1.0, Math.max(0.0, v)); }
}