package com.mycompany.passwordmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import java.io.IOException;
import static javafx.application.Application.launch;

/**
 * Application entry point for the Password Manager JavaFX UI.
 * Simple responsibility: boot the JavaFX runtime and load the login view.
 */
public class MainCLass extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    /**
     * Sets up the transparent stage and draggable scene, then reveals the login layout.
     *
     * @param stage primary stage supplied by the JavaFX runtime
     * @throws IOException if the login layout cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("login"));
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Password Manager");
        stage.setScene(scene);
        stage.setResizable(true);

        // ensure sizeToScene then center using exact width/height
        Platform.runLater(() -> {
            stage.sizeToScene();
            centerStage(stage);
        });

        final Delta dragDelta = new Delta();
        scene.setOnMousePressed(mouseEvent -> {
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        scene.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });

        stage.show();
        // also center after showing (defensive)
        Platform.runLater(() -> centerStage(stage));
    }
    /**
     * Minimal holder for the drag offset between the stage and the mouse pointer.
     */
    private static class Delta {
        double x, y;
    }
    /**
     * Replaces the current scene root with the requested FXML layout and resizes the stage.
     *
     * @param fxml logical FXML name without extension (e.g. {@code "login"})
     * @throws IOException if the FXML resource cannot be located or parsed
     */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        Platform.runLater(() -> {
            primaryStage.sizeToScene();
            centerStage(primaryStage);
        });
    }
    /**
     * Loads an FXML file from the application classpath.
     *
     * @param fxml logical FXML name without extension
     * @return loaded UI hierarchy
     * @throws IOException if the resource cannot be read
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    /**
     * Centers the given stage on the primary screen.
     *
     * @param stage stage to center
     */
    private static void centerStage(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double centerX = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2.0;
        double centerY = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2.0;
        stage.setX(centerX);
        stage.setY(centerY);
    }
    /**
     * Launches the JavaFX runtime.
     *
     * @param args CLI arguments (unused)
     */
    public static void main(String[] args) {
        launch();
    }
}
