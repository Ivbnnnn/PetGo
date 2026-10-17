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
import javafx.util.StringConverter;
import ru.mirea.petgo.dto.PetRow;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.UserService;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PetController {
    @FXML
    private TableView<PetRow> petsTable;
    @FXML
    private TableColumn<PetRow, Integer> idColumn;
    @FXML
    private TableColumn<PetRow, String> nameColumn;
    @FXML
    private TableColumn<PetRow, String> breedColumn;
    @FXML
    private TableColumn<PetRow, Object> weightColumn;
    @FXML
    private TableColumn<PetRow, Integer> ageColumn;
    @FXML
    private TableColumn<PetRow, String> ownerColumn;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<User> ownerFilter;
    @FXML
    private Label countLabel;
    @FXML
    private Label messageLabel;

    private PetService petService;
    private UserService userService;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        breedColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().breed()));
        weightColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().weight()));
        ageColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().age()));
        ownerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().ownerName()));
        ownerFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                return u == null ? "Все" : u.getName();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });

        searchField.setOnAction(e -> handleRefresh());
        ownerFilter.setOnAction(e -> handleRefresh());
    }

    public void setServices(PetService petService, UserService userService) {
        this.petService = petService;
        this.userService = userService;
        try {
            List<User> owners = userService.findAll().stream().filter(u -> u.getRole() == UserRole.OWNER).toList();
            ownerFilter.getItems().setAll(owners);
            handleRefresh();
        } catch (Exception e) {
            showError("Не удалось загрузить данные: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        if (petService == null)
            return;
        messageLabel.setText("");

        try {
            List<PetRow> rows = petService.findAllRows();
            String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                rows = rows.stream().filter(r -> (r.name() != null && r.name().toLowerCase().contains(search))
                        || (r.breed() != null && r.breed().toLowerCase().contains(search))).toList();
            }
            User owner = ownerFilter.getValue();
            if (owner != null) {
                rows = rows.stream().filter(r -> r.ownerId() == owner.getId()).toList();
            }

            petsTable.setItems(FXCollections.observableArrayList(rows));
            countLabel.setText(String.valueOf(rows.size()));
        } catch (Exception e) {
            showError("Ошибка загрузки: " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        ownerFilter.setValue(null);
        handleRefresh();
    }

    @FXML
    private void handleAdd() {
        openForm(null);
    }

    @FXML
    private void handleEdit() {
        PetRow row = petsTable.getSelectionModel().getSelectedItem();
        if (row == null)
            return;

        try {
            Pet pet = petService.getById(row.id());
            openForm(pet);
        } catch (Exception e) {
            showError("Не удалось открыть питомца: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        PetRow row = petsTable.getSelectionModel().getSelectedItem();
        if (row == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить питомца «" + row.name() + "»?",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            return;

        try {
            petService.deletePet(row.id());
            handleRefresh();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void openForm(Pet pet) {
        try {
            URL url = getClass().getResource("/fxml/pets-form.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            PetFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(pet == null ? "Новый питомец" : "Изменить питомца");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(450);
            stage.setMinHeight(400);

            controller.setDialogStage(stage);
            controller.setPet(pet);
            controller.setServices(petService, userService);

            stage.showAndWait();

            if (controller.isSaved()) {
                handleRefresh();
            }
        } catch (Exception e) {
            showError("Не удалось открыть форму: " + e.getMessage());
        }
    }

    private void showError(String message) {
        messageLabel.setText(message);
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
