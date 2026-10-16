package ru.mirea.petgo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.util.Labels;

import java.util.List;

public class UserFormController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private TextField addressField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label errorLabel;

    private UserService userService;
    private Stage dialogStage;
    private User userToEdit;
    private boolean saved;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        activeCheckBox.setSelected(true);

        roleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole role) {
                return role == null ? "" : Labels.role(role);
            }

            @Override
            public UserRole fromString(String value) {
                return null;
            }
        });
        roleComboBox.getItems().setAll(UserRole.values());
    }

    public void setServices(UserService userService) {
        this.userService = userService;
        if (userToEdit != null) {
            populateForm();
        }
    }

    public void setUser(User user) {
        this.userToEdit = user;
        if (user != null && userService != null) {
            populateForm();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void populateForm() {
        if (userToEdit == null) {
            return;
        }
        titleLabel.setText("Редактирование пользователя №" + userToEdit.getId());
        nameField.setText(userToEdit.getName());
        emailField.setText(userToEdit.getEmail());
        phoneField.setText(userToEdit.getPhone() == null ? "" : userToEdit.getPhone());
        addressField.setText(userToEdit.getAddress() == null ? "" : userToEdit.getAddress());
        roleComboBox.setValue(userToEdit.getRole());
        activeCheckBox.setSelected(userToEdit.isActive());
    }

    @FXML
    private void handleSave() {
        clearErrors();

        try {
            if (userService == null) {
                showError("Форма не подключена к сервисам.");
                return;
            }

            UserRole role = roleComboBox.getValue();
            if (role == null) {
                showFieldError(roleComboBox, "Выберите роль.");
                return;
            }

            User user = userToEdit == null ? new User() : userToEdit;
            user.setName(nameField.getText());
            user.setEmail(emailField.getText());
            user.setPhone(phoneField.getText());
            user.setAddress(addressField.getText());
            user.setRole(role);
            user.setActive(activeCheckBox.isSelected());

            if (userToEdit == null) {
                userService.create(user);
            } else {
                userService.update(user);
            }

            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } catch (ValidationException e) {
            Control field = switch (e.getField()) {
                case "name" -> nameField;
                case "email" -> emailField;
                case "phone" -> phoneField;
                case "role" -> roleComboBox;
                case "address" -> addressField;
                default -> null;
            };
            if (field != null) {
                showFieldError(field, e.getMessage());
            } else {
                showError(e.getMessage());
            }
        } catch (BusinessException e) {
            showError(e.getMessage());
        } catch (DatabaseException e) {
            showError("Ошибка базы данных: " + e.getMessage());
        } catch (Exception e) {
            showError("Не удалось сохранить: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showFieldError(Control field, String message) {
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        errorLabel.setText(message);
    }

    private void clearErrors() {
        errorLabel.setText("");
        for (Control c : List.of(nameField, emailField, phoneField, roleComboBox, addressField)) {
            c.getStyleClass().remove("field-error");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}