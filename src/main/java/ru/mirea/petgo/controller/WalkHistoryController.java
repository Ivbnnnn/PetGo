package ru.mirea.petgo.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.mirea.petgo.dto.WalkHistoryRow;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.WalkHistoryService;
import ru.mirea.petgo.service.WalkRequestService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WalkHistoryController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private TableView<WalkHistoryRow> historyTable;
    @FXML private TableColumn<WalkHistoryRow, Integer> idColumn;
    @FXML private TableColumn<WalkHistoryRow, Integer> requestIdColumn;
    @FXML private TableColumn<WalkHistoryRow, String> petColumn;
    @FXML private TableColumn<WalkHistoryRow, String> walkDateColumn;
    @FXML private TableColumn<WalkHistoryRow, Integer> durationColumn;
    @FXML private TableColumn<WalkHistoryRow, String> routeColumn;
    @FXML private TableColumn<WalkHistoryRow, String> ownerReviewColumn;
    @FXML private TableColumn<WalkHistoryRow, String> walkerReviewColumn;
    @FXML private TableColumn<WalkHistoryRow, Integer> ratingColumn;
    @FXML private TableColumn<WalkHistoryRow, String> completedAtColumn;

    @FXML private ComboBox<Integer> ratingFilter;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;

    private WalkHistoryService historyService;
    private WalkRequestService requestService;
    private PetService petService;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        requestIdColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().walkRequestId()));
        petColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().petName() == null ? "—" : c.getValue().petName()));
        walkDateColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().walkDateTime() == null ? "" : c.getValue().walkDateTime().format(DATE_FORMAT)));
        durationColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().actualDuration()));
        routeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().route() == null ? "—" : c.getValue().route()));
        ownerReviewColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().ownerReview() == null ? "—" : c.getValue().ownerReview()));
        walkerReviewColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().walkerReview() == null ? "—" : c.getValue().walkerReview()));
        ratingColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().rating()));
        completedAtColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().completedAt() == null ? "" : c.getValue().completedAt().format(DATE_FORMAT)));

        ratingFilter.getItems().setAll(1, 2, 3, 4, 5);
        ratingFilter.setOnAction(e -> handleRefresh());
    }

    public void setServices(WalkHistoryService historyService,
                            WalkRequestService requestService,
                            PetService petService) {
        this.historyService = historyService;
        this.requestService = requestService;
        this.petService = petService;
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        if (historyService == null) {
            return;
        }
        messageLabel.setText("");

        try {
            List<WalkHistoryRow> rows = historyService.findAllRows();

            Integer minRating = ratingFilter.getValue();
            if (minRating != null) {
                List<WalkHistoryRow> filtered = new ArrayList<>();
                for (WalkHistoryRow r : rows) {
                    if (r.rating() != null && r.rating() >= minRating) {
                        filtered.add(r);
                    }
                }
                rows = filtered;
            }

            historyTable.setItems(FXCollections.observableArrayList(rows));
            countLabel.setText(String.valueOf(rows.size()));
        } catch (DatabaseException e) {
            showError("Ошибка загрузки: " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        ratingFilter.setValue(null);
        handleRefresh();
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleEdit() {
        WalkHistoryRow row = historyTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        try {
            WalkHistory history = historyService.findById(row.id());
            openForm(history);
        } catch (Exception e) {
            showError("Не удалось открыть запись: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        WalkHistoryRow row = historyTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }

        if (!confirm("Удалить запись истории №" + row.id() + "?")) {
            return;
        }

        try {
            historyService.delete(row.id());
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось удалить: " + e.getMessage());
        }
    }

    private void openForm(WalkHistory history) {
        try {
            URL url = getClass().getResource("/fxml/walk-history-form.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            WalkHistoryFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(history == null ? "Новая запись истории" : "Изменить запись истории");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(500);
            stage.setMinHeight(550);

            controller.setDialogStage(stage);
            controller.setHistory(history);
            controller.setServices(historyService);

            stage.showAndWait();

            if (controller.isSaved()) {
                handleRefresh();
            }
        } catch (Exception e) {
            showError("Не удалось открыть форму: " + e.getMessage());
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showError(String message) {
        messageLabel.setText(message);
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}