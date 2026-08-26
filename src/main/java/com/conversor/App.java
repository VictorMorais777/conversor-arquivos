package com.conversor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("view/conversor-view.fxml")
        );

        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 480, 560);

        stage.setTitle("Conversor de Arquivos");
        stage.setScene(scene);
        stage.setMinWidth(460);
        stage.setMinHeight(560);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}