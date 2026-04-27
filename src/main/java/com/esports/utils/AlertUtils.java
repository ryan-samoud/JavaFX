package com.esports.utils;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AlertUtils {

    public static void showNotification(Stage stage, String message, boolean success) {
        Popup popup = new Popup();
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(300);
        root.setPadding(new Insets(15, 20, 15, 20));
        String baseStyle = "-fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);";
        String colorStyle = success
                ? "-fx-background-color: #00ff9d; -fx-border-color: #00cc7d; -fx-border-width: 2;"
                : "-fx-background-color: #ff4757; -fx-border-color: #d63031; -fx-border-width: 2;";
        root.setStyle(baseStyle + colorStyle);
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: " + (success ? "#003300" : "#ffffff") + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        root.getChildren().add(label);
        popup.getContent().add(root);
        popup.setAutoHide(true);
        Scene scene = stage.getScene();
        if (scene != null) {
            double x = stage.getX() + (stage.getWidth() - root.getPrefWidth()) / 2;
            double y = stage.getY() + 50;
            popup.show(stage, x, y);
        } else {
            popup.show(stage);
        }
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        TranslateTransition moveUp = new TranslateTransition(Duration.millis(300), root);
        moveUp.setFromY(20); moveUp.setToY(0);
        fadeIn.play(); moveUp.play();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(3));
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
    }
}
