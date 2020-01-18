package io.freedriver.autonomy.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloFX extends Application {

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label l = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        Label l2 = new Label("Hello2, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");



        VBox vb = new VBox();
        vb.getChildren().add(l);
        Scene scene = new Scene(vb, 640, 480);

        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> vb.getChildren().add(l2));

    }

    public static void main(String[] args) {
        launch();
    }

}