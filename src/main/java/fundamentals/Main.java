package fundamentals;

import controllers.Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Core;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("views/main.fxml"));
        BorderPane root = fxmlLoader.load();
        root.setPrefWidth(700.0d);
        root.setPrefHeight(400.0d);

        primaryStage.setTitle("The Marksman v1.0.0 (BETA)");
        Scene scene = new Scene(root, 619, 377);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("styles/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        Controller controller = fxmlLoader.getController();

        primaryStage.setOnCloseRequest(event -> controller.stopCamera());
    }

    public static void main(String[] args) {
        launch(args);
    }

    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
}
