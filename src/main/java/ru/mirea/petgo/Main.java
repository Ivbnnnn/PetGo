package ru.mirea.petgo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main {
    public static void main(String[] args) {
        Application.launch(HelloWorldApp.class, args);
    }

    public static class HelloWorldApp extends Application {
        @Override
        public void start(Stage stage) {
            Label greeting = new Label("Hello World");
            StackPane root = new StackPane(greeting);

            stage.setTitle("PetGo");
            stage.setScene(new Scene(root, 480, 320));
            stage.show();
        }
    }
}
