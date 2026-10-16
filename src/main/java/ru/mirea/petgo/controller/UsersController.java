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
import ru.mirea.petgo.dto.UserRow;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.util.Labels;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UsersController {

    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, Integer> idColumn;
    @FXML private TableColumn<UserRow, String> nameColumn;
    @FXML private TableColumn<UserRow, String> emailColumn;
    @FXML private TableColumn<UserRow, String> phoneColumn;
    @FXML private TableColumn<UserRow, String> roleColumn;
    @FXML private TableColumn<UserRow, String> addressColumn;
    @FXML private TableColumn<UserRow, Boolean> activeColumn;

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;
    @FXML private Label detailsLabel;

    private UserService userService;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().email()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().phone()));
        roleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Labels.role(c.getValue().role())));
        addressColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().address()));
        activeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().active()));

        searchField.setOnAction(e -> handleRefresh());

        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldRow, row) -> showDetails(row));
    }

    public void setServices(UserService userService) {
        this.userService = userService;
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        if (userService == null) {
            return;
        }
        messageLabel.setText("");

        try {
            List<UserRow> rows = userService.findAllRows();

            String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                rows = new ArrayList<>(rows.stream()
                        .filter(r -> (r.name() != null && r.name().toLowerCase().contains(search))
                                || (r.email() != null && r.email().toLowerCase().contains(search)))
                        .toList());
            }

            usersTable.setItems(FXCollections.observableArrayList(rows));
            countLabel.setText(String.valueOf(rows.size()));
            showDetails(null);
        } catch (DatabaseException e) {
            showError("Ошибка загрузки: " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        handleRefresh();
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleEdit() {
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        try {
            User user = userService.findById(row.id());
            openForm(user);
        } catch (Exception e) {
            showError("Не удалось открыть пользователя: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        UserRow row = usersTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }

        if (!confirm("Удалить пользователя «" + row.name() + "»?")) {
            return;
        }

        try {
            userService.delete(row.id());
            handleRefresh();
        } catch (BusinessException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Не удалось удалить: " + e.getMessage());
        }
    }

    private void openForm(User user) {
        try {
            URL url = getClass().getResource("/fxml/user-form.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            UserFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(user == null ? "Новый пользователь" : "Изменить пользователя");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(450);
            stage.setMinHeight(450);

            controller.setDialogStage(stage);
            controller.setUser(user);
            controller.setServices(userService);

            stage.showAndWait();

            if (controller.isSaved()) {
                handleRefresh();
            }
        } catch (Exception e) {
            showError("Не удалось открыть форму: " + e.getMessage());
        }
    }

    private void showDetails(UserRow row) {
        if (row == null) {
            detailsLabel.setText("Выберите пользователя для просмотра.");
            return;
        }
        detailsLabel.setText(
                "Имя: " + row.name()
                + "\nEmail: " + row.email()
                + "\nТелефон: " + (row.phone() == null ? "—" : row.phone())
                + "\nРоль: " + Labels.role(row.role())
                + "\nАдрес: " + (row.address() == null ? "—" : row.address())
                + "\nАктивен: " + (row.active() ? "да" : "нет"));
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