package com.conversor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.InputStream;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("view/conversor-view.fxml")
        );

        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 480, 560);

        stage.setTitle("Conversor de Arquivos");
        aplicarIcone(stage);
        stage.setScene(scene);
        stage.setMinWidth(460);
        stage.setMinHeight(560);
        stage.setResizable(true);
        stage.show();
    }

    private void aplicarIcone(Stage stage) {
        try (InputStream icone = getClass().getResourceAsStream("icone.png")) {
            if (icone != null) {
                stage.getIcons().add(new Image(icone));
            } else {
                System.err.println("Aviso: icone.png não encontrado em resources/com/conversor/. Usando ícone padrão.");
            }
        } catch (Exception e) {
            System.err.println("Aviso: não foi possível carregar o ícone. Usando ícone padrão. Detalhe: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}