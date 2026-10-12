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
import ru.mirea.petgo.model.WalkRequest;
import ru.mirea.petgo.model.enums.UserRole;
import ru.mirea.petgo.service.PetService;
import ru.mirea.petgo.service.UserService;
import ru.mirea.petgo.service.WalkRequestService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class WalkRequestFormController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label titleLabel;
    @FXML private ComboBox<User> ownerComboBox;
    @FXML private ComboBox<Pet> petComboBox;
    @FXML private ComboBox<User> walkerComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField durationField;
    @FXML private TextField priceField;
    @FXML private TextField addressField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField statusField;
    @FXML private Label errorLabel;

    private WalkRequestService requestService;
    private PetService petService;
    private UserService userService;

    private Stage dialogStage;
    private WalkRequest requestToEdit;
    private boolean saved;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        datePicker.setValue(LocalDate.now().plusDays(1));
        statusField.setText("Создана");

        ownerComboBox.setConverter(new StringConverter<>() {
            @Override 
            public String toString(User user) {
                return user == null ? "":user.getName();
            }

            @Override 
            public User fromString(String value) {
                return  null;
            }
        });

        walkerComboBox.setConverter(new StringConverter<>() {
            @Override 
            public String toString(User user) {
                return user == null ? "":user.getName();
            }

            @Override 
            public User fromString(String value){
                return null;
            }
        });

        petComboBox.setConverter(new StringConverter<>() {
            @Override 
            public String toString(Pet pet){
                return pet == null ? "":pet.getName();
            }

            @Override 
            public Pet fromString(String value) {
                return null;
            }
        });

        ownerComboBox.valueProperty().addListener(
                (obs, oldOwner, newOwner) -> loadPets(newOwner)
        );
    }

    public void setServices(
        WalkRequestService requestService,
        PetService petService,
        UserService userService
    ) {
        this.requestService = Objects.requireNonNull(requestService);
        this.petService = Objects.requireNonNull(petService);
        this.userService = Objects.requireNonNull(userService);

        loadUsers();

        if (requestToEdit != null) {
            populateForm();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setRequest(WalkRequest request) {
        this.requestToEdit = request;

        if (request != null) {
            populateForm();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void loadUsers() {
        try {
            List<User> users = userService.findAll();

            ownerComboBox.getItems().setAll(
                users.stream().filter(User::isActive).filter(u -> u.getRole() == UserRole.OWNER).toList()
            );

            walkerComboBox.getItems().setAll(
                    users.stream().filter(User::isActive).filter(u -> u.getRole() == UserRole.WALKER).toList()
            );

            if (requestToEdit != null) {
                populateForm();
            }
        } catch (DatabaseException e) {
            showError( "Не удалось загрузить пользователей из базы данных.");
        }
    }

    private void loadPets(User owner) {
        petComboBox.getItems().clear();

        if (owner == null || petService == null) {
            return;
        }

        try {
            petComboBox.getItems().setAll(petService.getByOwner(owner.getId()));
            
            if (requestToEdit != null && requestToEdit.getOwnerId() == owner.getId()) {
                petComboBox.getItems().stream().filter(p -> p.getId() == requestToEdit.getPetId()).findFirst().ifPresent(petComboBox::setValue);
            }
        } catch (DatabaseException e) {
            showError("Не удалось загрузить питомцев выбранного владельца.");
        }
    }

    private void populateForm() {
        if (requestToEdit == null || userService == null) {
            return;
        }

        ownerComboBox.getItems().stream().filter(u -> u.getId() == requestToEdit.getOwnerId()).findFirst().ifPresent(ownerComboBox::setValue);

        if (requestToEdit.getWalkerId() != null) {
            walkerComboBox.getItems().stream().filter(u -> u.getId() == requestToEdit.getWalkerId()).findFirst().ifPresent(walkerComboBox::setValue);
        }

        LocalDateTime dateTime = requestToEdit.getWalkDateTime();

        if (dateTime != null) {
            datePicker.setValue(dateTime.toLocalDate());
            timeField.setText(dateTime.toLocalTime().format(TIME_FORMAT));
        }

        durationField.setText(String.valueOf(requestToEdit.getDurationMinutes()));

        priceField.setText(requestToEdit.getPrice() == null ? "" : requestToEdit.getPrice().toPlainString());

        addressField.setText(requestToEdit.getWalkAddress() == null ? "" : requestToEdit.getWalkAddress());

        descriptionArea.setText(requestToEdit.getDescription() == null ? "" : requestToEdit.getDescription());

        statusField.setText(requestToEdit.getStatus() == null ? "" : russianStatus(requestToEdit));
 
        titleLabel.setText( "Редактирование заявки №" + requestToEdit.getId());
    }

    @FXML
    private void handleSave() {
        clearErrors();

        try {
            if (requestService == null || petService == null || userService == null) {

                showError("Форма не подключена к сервисам приложения.");
                return;
            }

            User owner = ownerComboBox.getValue();
            Pet pet = petComboBox.getValue();
            User walker = walkerComboBox.getValue();

            if (owner == null) {
                showFieldError(ownerComboBox, "Выберите владельца.");
                return;
            }

            if (pet == null) {
                showFieldError(petComboBox, "Выберите питомца.");
                return;
            }

            if (pet.getOwnerId() != owner.getId()) {
                showFieldError(petComboBox,"Выбранный питомец не принадлежит этому владельцу.");
                return;
            }

            if (walker == null) {
                showFieldError(walkerComboBox, "Выберите выгульщика.");
                return;
            }

            if (datePicker.getValue() == null) {
                showFieldError(datePicker, "Выберите дату.");
                return;
            }

            

            LocalTime time;

            try {
                time = LocalTime.parse(
                        timeField.getText().trim(),
                        TIME_FORMAT
                );
            } catch (RuntimeException e) {
                showFieldError(
                        timeField,
                        "Введите время в формате ЧЧ:ММ, например 09:30."
                );
                return;
            }
            

            LocalDateTime selectedDateTime = LocalDateTime.of(
                    datePicker.getValue(), time
            );

            if (requestToEdit == null && selectedDateTime.isBefore(LocalDateTime.now())) {

                showFieldError(
                        datePicker, "Нельзя создать заявку на прошедшие дату и время.");
            return;
            }


            int duration;

            try {
                duration = Integer.parseInt(
                        durationField.getText().trim()
                );
            } catch (RuntimeException e) {
                showFieldError(
                        durationField,
                        "Длительность должна быть целым числом."
                );
                return;
            }

            if (duration <= 0) {
                showFieldError(
                        durationField,
                        "Длительность должна быть больше нуля."
                );
                return;
            }

            BigDecimal price;

            try {
                price = new BigDecimal(priceField.getText().trim());
            } catch (RuntimeException e) {
                showFieldError(priceField, "Введите корректную цену.");
                return;
            }

            if (price.signum() < 0
                    || price.precision() - price.scale() > 8
                    || price.scale() > 2) {

                showFieldError(
                        priceField,
                        "Цена должна быть неотрицательной и содержать "
                                + "не более двух знаков после запятой."
                );
                return;
            }

            String address = addressField.getText() == null
                    ? ""
                    : addressField.getText().trim();

            if (address.isBlank()) {
                showFieldError(addressField, "Введите адрес.");
                return;
            }

            if (address.length() > 200) {
                showFieldError(
                        addressField,
                        "Адрес не должен превышать 200 символов."
                );
                return;
            }

            WalkRequest request = requestToEdit == null
                    ? new WalkRequest()
                    : requestToEdit;

            request.setOwnerId(owner.getId());
            request.setPetId(pet.getId());
            request.setWalkerId(walker.getId());

            request.setWalkDateTime(
                    LocalDateTime.of(datePicker.getValue(), time)
            );

            request.setDurationMinutes(duration);
            request.setPrice(price);
            request.setWalkAddress(address);

            request.setDescription(
                    descriptionArea.getText() == null
                            ? ""
                            : descriptionArea.getText().trim()
            );

            if (requestToEdit == null) {
                requestService.create(request);
            } else {
                requestService.update(request);
            }

            saved = true;

            if (dialogStage != null) {
                dialogStage.close();
            }


        } catch (ValidationException e) {

            Control field = switch (e.getField()) {
                case "pet", "petId" -> petComboBox;
                case "owner", "ownerId" -> ownerComboBox;
                case "walker", "walkerId" -> walkerComboBox;
                case "walkDateTime", "date" -> datePicker;
                case "duration", "durationMinutes" -> durationField;
                case "price" -> priceField;
                case "walkAddress", "address" -> addressField;
                case "description" -> descriptionArea;
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
            e.printStackTrace();

            showError(
                "Ошибка сохранения в базе данных. " +
                "Подробности выведены в терминал."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showError(
                "Не удалось сохранить заявку. " +
                "Подробности выведены в терминал."
            );
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

        showError(message);
    }

    private void clearErrors() {
        errorLabel.setText("");

        for (Control control : List.of(
                ownerComboBox,
                petComboBox,
                walkerComboBox,
                datePicker,
                timeField,
                durationField,
                priceField,
                addressField
        )) {
            control.getStyleClass().remove("field-error");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private String russianStatus(WalkRequest request) {
        if (request.getStatus() == null) {
            return "";
        }

        return switch (request.getStatus()) {
            case CREATED -> "Создана";
            case IN_PROGRESS -> "В процессе";
            case COMPLETED -> "Завершена";
            case CANCELLED -> "Отменена";
        };
    }
}

