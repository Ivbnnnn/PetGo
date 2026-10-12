
package ru.mirea.petgo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import ru.mirea.petgo.controller.WalkRequestsController;
import ru.mirea.petgo.repository.*;
import ru.mirea.petgo.service.*;
import ru.mirea.petgo.util.DatabaseManager;

import java.sql.Connection;

public class Main extends Application {

    private Connection connection;

    @Override
    public void start(Stage stage) throws Exception {

        connection = DatabaseManager.getConnection();

        UserRepository userRepository =
                new UserRepository(connection);

        PetRepository petRepository =
                new PetRepository(connection);

        WalkRequestRepository requestRepository =
                new WalkRequestRepository(connection);

        WalkRequestService requestService =
                new WalkRequestService(
                        requestRepository,
                        petRepository,
                        userRepository
                );

        PetService petService =
                new PetService(
                        petRepository,
                        userRepository,
                        requestRepository
                );

        UserService userService =
                new UserService(
                        userRepository,
                        petRepository,
                        requestRepository
                );

        ExportService exportService = new ExportService();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/fxml/walk-requests.fxml"
                )
        );

        Parent root = loader.load();

        WalkRequestsController controller =
                loader.getController();

        controller.setServices(
                requestService,
                petService,
                userService,
                exportService
        );

        Scene scene = new Scene(root, 1200, 750);

        stage.setTitle("PetGo — Заявки на выгул");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
