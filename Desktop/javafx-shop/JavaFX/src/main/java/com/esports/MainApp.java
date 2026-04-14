package com.esports;

import com.esports.utils.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static final String APP_TITLE = "NEXUS ESPORTS";
    private static final int    WIDTH     = 1280;
    private static final int    HEIGHT    = 800;

    @Override
    public void start(Stage primaryStage) throws Exception {


        DatabaseConnection.getInstance();
        System.out.println("DB = " + DatabaseConnection.getInstance());
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/esports/fxml/HomeView.fxml") // ← page d'accueil
        );

        Scene scene = new Scene(loader.load(), WIDTH, HEIGHT);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Fermer proprement la connexion MySQL à la fermeture de l'app
        DatabaseConnection.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}