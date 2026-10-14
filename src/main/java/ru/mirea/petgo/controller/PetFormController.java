package ru.mirea.petgo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.Pet;
import ru.mirea.petgo.model.User;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.UserService;

import java.math.BigDecimal;
import java.util.List;

public class PetFormController {
    @FXML
    private Label titleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField breedField;
    @FXML
    private TextField weightField;
    @FXML
    private TextField ageField;
    @FXML
    private TextArea needsArea;
    @FXML
    private ComboBox<User> ownerComboBox;
    @FXML
    private TextField photoField;
    @FXML
    private Label errorLabel;

    private PetService petService;
    private UserService userService;
    private Stage dialogStage;
    private Pet petToEdit;
    private boolean saved;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        ownerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                return u == null ? "" : u.getName();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
    }

    public void setServices(PetService petService, UserService userService) {
        this.petService = petService;
        this.userService = userService;

        try {
            List<User> owners = userService.findAll().stream().filter(u -> u.getRole() == UserRole.OWNER).toList();
            ownerComboBox.getItems().setAll(owners);
            if (petToEdit != null) {
                populateForm();
            }
        } catch (DatabaseException e) {
            errorLabel.setText("Не удалось загрузить владельцев");
        }
    }

    public void setPet(Pet pet) {
        this.petToEdit = pet;
        if (pet != null && !ownerComboBox.getItems().isEmpty()) {
            populateForm();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void populateForm() {
        if (petToEdit == null)
            return;

        titleLabel.setText("Редактирование питомца №" + petToEdit.getId());
        nameField.setText(petToEdit.getName());
        breedField.setText(petToEdit.getBreed());
        weightField.setText(petToEdit.getWeight() == null ? "" : petToEdit.getWeight().toPlainString());
        ageField.setText(petToEdit.getAge() == null ? "" : String.valueOf(petToEdit.getAge()));
        needsArea.setText(petToEdit.getSpecialNeeds());
        photoField.setText(petToEdit.getPhotoUrl());

        ownerComboBox.getItems().stream().filter(u -> u.getId() == petToEdit.getOwnerId()).findFirst()
                .ifPresent(ownerComboBox::setValue);
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        clearFieldErrors();
        try {
            User owner = ownerComboBox.getValue();
            if (owner == null) {
                showFieldError(ownerComboBox, "Выберите владельца");
                return;
            }

            Pet pet = petToEdit == null ? new Pet() : petToEdit;
            pet.setName(nameField.getText());
            pet.setBreed(breedField.getText());
            pet.setSpecialNeeds(needsArea.getText());
            pet.setPhotoUrl(photoField.getText());
            pet.setOwnerId(owner.getId());

            String weightText = weightField.getText();
            pet.setWeight(weightText == null || weightText.isBlank() ? null
                    : new BigDecimal(weightText.trim().replace(',', '.')));
            String ageText = ageField.getText();
            pet.setAge(ageText == null || ageText.isBlank() ? null : Integer.parseInt(ageText.trim()));
            if (petToEdit == null) {
                petService.createPet(pet);
            } else {
                petService.updatePet(pet);
            }
            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } catch (ValidationException e) {
            Control field = switch (e.getField()) {
                case "name" -> nameField;
                case "breed" -> breedField;
                case "weight" -> weightField;
                case "age" -> ageField;
                case "owner" -> ownerComboBox;
                case "photoUrl" -> photoField;
                default -> null;
            };
            if (field != null) {
                showFieldError(field, e.getMessage());
            } else {
                errorLabel.setText(e.getMessage());
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Введите корректное число");
        } catch (BusinessException e) {
            errorLabel.setText(e.getMessage());
        } catch (DatabaseException e) {
            errorLabel.setText("Ошибка БД: " + e.getMessage());
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

    private void clearFieldErrors() {
        for (Control c : List.of(nameField, breedField, weightField, ageField, ownerComboBox, photoField)) {
            c.getStyleClass().remove("field-error");
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }
}
