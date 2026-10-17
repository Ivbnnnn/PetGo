package ru.mirea.petgo.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.layout.BorderPane;
import ru.mirea.petgo.service.ExportService;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.StatisticsService;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;
import javafx.scene.Parent;

public class MainController {

    @FXML private BorderPane mainPane;

    private PetService petService;
    private UserService userService;
    private StatisticsService statisticsService;
    private ExportService exportService;
    private WalkHistoryService walkHistoryService;
    private WalkRequestService walkRequestService;

    public void setServices(PetService petService, UserService userService, StatisticsService statisticsService,
        ExportService exportService,WalkHistoryService walkHistoryService,WalkRequestService walkRequestService
    ) {
        this.petService = petService;
        this.userService = userService;
        this.exportService = exportService;
        this.statisticsService = statisticsService;
        this.walkHistoryService = walkHistoryService;
        this.walkRequestService = walkRequestService;
        showPets(); // экран при запуске
    }

    @FXML
    private void showPets() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/pets.fxml")
            );
            Parent view = loader.load();

            PetController controller = loader.getController();
            controller.setServices(petService, userService);

            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/users.fxml")
            );
            Parent view = loader.load();

            UsersController controller = loader.getController();
            controller.setServices(userService);

            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showWalkHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/walk-history.fxml")
            );
            Parent view = loader.load();

            WalkHistoryController controller = loader.getController();
            controller.setServices(walkHistoryService, walkRequestService, petService);

            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showWalkRequests() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/walk-requests.fxml")
            );
            Parent view = loader.load();

            WalkRequestsController controller = loader.getController();
            controller.setServices(walkRequestService, petService, userService, exportService);

            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/statistics.fxml")
            );
            Parent view = loader.load();

            StatisticsController controller = loader.getController();
            controller.setServices(statisticsService);

            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
