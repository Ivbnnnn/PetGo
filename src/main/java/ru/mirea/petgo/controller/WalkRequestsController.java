
package ru.mirea.petgo.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.mirea.petgo.dto.WalkRequestFilter;
import ru.mirea.petgo.dto.WalkRequestRow;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.model.enums.WalkStatus;
import ru.mirea.petgo.service.ExportService;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.service.WalkRequestService;
import ru.mirea.petgo.util.Labels;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WalkRequestsController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private TableView<WalkRequestRow> requestsTable;
    @FXML private TableColumn<WalkRequestRow, String> petColumn;
    @FXML private TableColumn<WalkRequestRow, String> ownerColumn;
    @FXML private TableColumn<WalkRequestRow, String> walkerColumn;
    @FXML private TableColumn<WalkRequestRow, String> dateColumn;
    @FXML private TableColumn<WalkRequestRow, Integer> durationColumn;
    @FXML private TableColumn<WalkRequestRow, String> addressColumn;
    @FXML private TableColumn<WalkRequestRow, String> statusColumn;
    @FXML private TableColumn<WalkRequestRow, BigDecimal> priceColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<WalkStatus> statusFilter;
    @FXML private ComboBox<User> walkerFilter;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private TextField priceFrom;
    @FXML private TextField priceTo;
    @FXML private ComboBox<String> sortBox;
    @FXML private CheckBox ascendingBox;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;
    @FXML private Label detailsLabel;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button startButton;
    @FXML private Button completeButton;
    @FXML private Button cancelButton;

    private WalkRequestService requestService;
    private PetService petService;
    private UserService userService;
    private ExportService exportService;

    @FXML
    private void initialize() {
        petColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().petName()));

        ownerColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().ownerName()));

        walkerColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().walkerName() == null
                                ? "Не назначен"
                                : c.getValue().walkerName()
                ));

        dateColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().walkDateTime() == null
                                ? ""
                                : c.getValue().walkDateTime()
                                        .format(DATE_FORMAT)
                ));

        durationColumn.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().durationMinutes()
                ));

        addressColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().walkAddress()
                ));

        statusColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Labels.status(c.getValue().status())
                ));

        priceColumn.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().price()
                ));

        statusFilter.getItems().setAll(WalkStatus.values());

        sortBox.getItems().setAll(
                "Дата", "Длительность", "Цена",
                "Статус", "Питомец"
        );
        sortBox.setValue("Дата");

        requestsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldRow, row) -> {
                    updateButtons(row);
                    showDetails(row);
                });

        searchField.setOnAction(e -> handleRefresh());
        statusFilter.setOnAction(e -> handleRefresh());
        walkerFilter.setOnAction(e -> handleRefresh());
        sortBox.setOnAction(e -> handleRefresh());
        ascendingBox.setOnAction(e -> handleRefresh());

        updateButtons(null);
    }

    public void setServices(
            WalkRequestService requestService,
            PetService petService,
            UserService userService,
            ExportService exportService
    ) {
        this.requestService = requestService;
        this.petService = petService;
        this.userService = userService;
        this.exportService = exportService;

        try {
            List<User> walkers = userService.findAll()
                    .stream()
                    .filter(u -> u.getRole() == UserRole.WALKER)
                    .toList();

            walkerFilter.getItems().setAll(walkers);

            walkerFilter.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(User user) {
                    return user == null ? "" : user.getName();
                }

                @Override
                public User fromString(String value) {
                    return null;
                }
            });

            handleRefresh();

        } catch (Exception e) {
            showError("Не удалось загрузить данные.");
        }
    }

    private String sortKey() {
        return switch (sortBox.getValue()) {
            case "Длительность" -> "duration";
            case "Цена" -> "price";
            case "Статус" -> "status";
            case "Питомец" -> "pet";
            default -> "date";
        };
    }

    private BigDecimal parsePrice(TextField field) {
        String text = field.getText().trim().replace(',', '.');
        if (text.isEmpty()) {
            return null;
        }
        return new BigDecimal(text);
    }

    @FXML
    private void handleRefresh() {
        if (requestService == null) {
            return;
        }

        try {
            WalkRequestFilter filter = new WalkRequestFilter(
                    searchField.getText(),
                    statusFilter.getValue(),
                    walkerFilter.getValue() == null
                            ? null
                            : walkerFilter.getValue().getId(),
                    dateFrom.getValue(),
                    dateTo.getValue(),
                    parsePrice(priceFrom),
                    parsePrice(priceTo),
                    sortKey(),
                    ascendingBox.isSelected()
            );

            requestsTable.setItems(
                    FXCollections.observableArrayList(
                            requestService.search(filter)
                    )
            );

            countLabel.setText(
                    String.valueOf(requestsTable.getItems().size())
            );

            messageLabel.setText("Данные обновлены");
            updateButtons(null);
            showDetails(null);

        } catch (NumberFormatException e) {
            showError("Введите корректную цену.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Не удалось загрузить заявки.");
        }
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        walkerFilter.setValue(null);
        dateFrom.setValue(null);
        dateTo.setValue(null);
        priceFrom.clear();
        priceTo.clear();
        sortBox.setValue("Дата");
        ascendingBox.setSelected(false);
        handleRefresh();
    }

    private WalkRequestRow selected() {
        return requestsTable.getSelectionModel()
                .getSelectedItem();
    }

    private void updateButtons(WalkRequestRow row) {
        boolean missing = row == null;

        editButton.setDisable(missing);
        deleteButton.setDisable(missing);

        startButton.setDisable(
                missing || row.status() != WalkStatus.CREATED
                        || row.walkerId() == null
        );

        completeButton.setDisable(
                missing || row.status() != WalkStatus.IN_PROGRESS
        );

        cancelButton.setDisable(
                missing || (
                        row.status() != WalkStatus.CREATED
                        && row.status() != WalkStatus.IN_PROGRESS
                )
        );
    }

    private void showDetails(WalkRequestRow row) {
        if (row == null) {
            detailsLabel.setText("Выберите заявку для просмотра.");
            return;
        }

        detailsLabel.setText(
                "Питомец: " + row.petName()
                + "\nВладелец: " + row.ownerName()
                + "\nВыгульщик: " +
                (row.walkerName() == null ? "Не назначен" : row.walkerName())
                + "\nАдрес: " + row.walkAddress()
                + "\nОписание: " +
                (row.description() == null ? "—" : row.description())
        );
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleEdit() {
        if (selected() == null) {
            return;
        }

        try {
            openForm(requestService.findById(selected().id()));
        } catch (Exception e) {
            showError("Не удалось открыть заявку.");
        }
    }

    private void openForm(WalkRequest request) {
        try {
            URL url = getClass().getResource(
                    "/fxml/walk-request-form.fxml"
            );

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            WalkRequestFormController controller =
                    loader.getController();

            Stage stage = new Stage();
            stage.setTitle(
                    request == null ? "Новая заявка" : "Изменить заявку"
            );
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            controller.setDialogStage(stage);
            controller.setRequest(request);
            controller.setServices(
                    requestService, petService, userService
            );

            stage.showAndWait();

            if (controller.isSaved()) {
                handleRefresh();
            }

        } catch (Exception e) {
            showError("Не удалось открыть форму заявки.");
        }
    }

    @FXML
    private void handleDelete() {
        WalkRequestRow row = selected();
        if (row == null) {
            return;
        }

        if (!confirm("Удалить выбранную заявку?")) {
            return;
        }

        try {
            requestService.delete(row.id());
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось удалить заявку: " + e.getMessage());
        }
    }

    @FXML
    private void handleStart() {
        WalkRequestRow row = selected();
        if (row == null || row.walkerId() == null) {
            return;
        }

        try {
            requestService.startWalk(row.id(), row.walkerId());
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось начать прогулку: " + e.getMessage());
        }
    }

    @FXML
    private void handleComplete() {
        WalkRequestRow row = selected();
        if (row == null || row.walkerId() == null) {
            return;
        }

        if (!confirm("Завершить прогулку?")) {
            return;
        }

        try {
            requestService.completeWalk(row.id(), row.walkerId());
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось завершить прогулку: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        WalkRequestRow row = selected();
        if (row == null) {
            return;
        }

        if (!confirm("Отменить заявку?")) {
            return;
        }

        try {
            requestService.cancelRequest(row.id(), row.ownerId());
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось отменить заявку: " + e.getMessage());
        }
    }

    @FXML
    private void handleExport() {
        if (exportService == null) {
            showError("Сервис экспорта не подключён.");
            return;
        }

        List<WalkRequestRow> rows = new ArrayList<>(
                requestsTable.getItems()
        );

        if (rows.isEmpty()) {
            showError("В таблице нет данных для экспорта.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт заявок");
        chooser.setInitialFileName("petgo_requests.xlsx");

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Excel (*.xlsx)", "*.xlsx"
                ),
                new FileChooser.ExtensionFilter(
                        "CSV (*.csv)", "*.csv"
                )
        );

        File file = chooser.showSaveDialog(
                requestsTable.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        try {
            String extension = chooser.getSelectedExtensionFilter()
                    .getExtensions().get(0).substring(1);

            if (!file.getName().toLowerCase()
                    .endsWith(extension)) {
                file = new File(file.getAbsolutePath() + extension);
            }

            int count = exportService.export(rows, file);

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Экспортировано заявок: " + count
            ).showAndWait();

        } catch (Exception e) {
            showError("Ошибка экспорта: " + e.getMessage());
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.YES,
                ButtonType.NO
        );

        return alert.showAndWait()
                .orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showError(String message) {
        messageLabel.setText(message);
        new Alert(
                Alert.AlertType.ERROR,
                message
        ).showAndWait();
    }
}