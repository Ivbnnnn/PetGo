package ru.mirea.petgo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.StatisticsService;
import ru.mirea.petgo.util.Labels;

import java.util.Map;

public class StatisticsController {

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label ownersLabel;
    @FXML
    private Label walkersLabel;
    @FXML
    private Label petsLabel;
    @FXML
    private Label requestsLabel;
    @FXML
    private Label avgPriceLabel;
    @FXML
    private Label topWalkerLabel;
    @FXML
    private VBox statusBox;
    @FXML
    private Label errorLabel;

    private StatisticsService statisticsService;

    public void setServices(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        if (statisticsService == null) {
            return;
        }
        errorLabel.setText("");

        try {
            totalUsersLabel.setText(String.valueOf(statisticsService.totalUsers()));
            ownersLabel.setText(String.valueOf(statisticsService.countOwners()));
            walkersLabel.setText(String.valueOf(statisticsService.countWalkers()));
            petsLabel.setText(String.valueOf(statisticsService.countPets()));
            requestsLabel.setText(String.valueOf(statisticsService.countRequests()));
            avgPriceLabel.setText(statisticsService.averagePrice() + " ₽");

            User top = statisticsService.mostActiveWalker();
            topWalkerLabel.setText(top == null ? "нет данных" : top.getName());

            statusBox.getChildren().clear();
            Map<WalkStatus, Integer> byStatus = statisticsService.requestsByStatus();
            for (Map.Entry<WalkStatus, Integer> entry : byStatus.entrySet()) {
                statusBox.getChildren().add(
                        new Label(Labels.status(entry.getKey()) + ": " + entry.getValue()));
            }
        } catch (Exception e) {
            errorLabel.setText("Ошибка статистики: " + e.getMessage());
        }
    }
}