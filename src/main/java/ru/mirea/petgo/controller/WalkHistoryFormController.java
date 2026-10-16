package ru.mirea.petgo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.petgo.dto.WalkRequestOption;
import ru.mirea.petgo.exception.BusinessException;
import ru.mirea.petgo.exception.DatabaseException;
import ru.mirea.petgo.exception.ValidationException;
import ru.mirea.petgo.model.WalkHistory;
import ru.mirea.petgo.service.WalkHistoryService;

import java.util.List;

public class WalkHistoryFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<WalkRequestOption> requestComboBox;
    @FXML private TextField durationField;
    @FXML private TextField routeField;
    @FXML private TextArea ownerReviewArea;
    @FXML private TextArea walkerReviewArea;
    @FXML private Spinner<Integer> ratingSpinner;
    @FXML private Label errorLabel;

    private WalkHistoryService historyService;
    private Stage dialogStage;
    private WalkHistory historyToEdit;
    private boolean saved;

    @FXML
    private void initialize() {
        errorLabel.setText("");
        ratingSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));

        requestComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WalkRequestOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public WalkRequestOption fromString(String value) {
                return null;
            }
        });
    }

    public void setServices(WalkHistoryService historyService) {
        this.historyService = historyService;

        try {
            if (historyToEdit == null) {
                List<WalkRequestOption> options = historyService.findCompletedWithoutHistory();
                requestComboBox.getItems().setAll(options);
            }
        } catch (DatabaseException e) {
            errorLabel.setText("Не удалось загрузить список заявок");
        }

        if (historyToEdit != null) {
            populateForm();
        }
    }

    public void setHistory(WalkHistory history) {
        this.historyToEdit = history;
        if (history != null && historyService != null) {
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
        if (historyToEdit == null) {
            return;
        }

        titleLabel.setText("Редактирование записи №" + historyToEdit.getId());

        requestComboBox.setDisable(true);
        requestComboBox.setPromptText("Заявка №" + historyToEdit.getWalkRequestId() + " (нельзя изменить)");

        durationField.setText(historyToEdit.getActualDuration() == null
                ? "" : String.valueOf(historyToEdit.getActualDuration()));
        routeField.setText(historyToEdit.getRoute() == null ? "" : historyToEdit.getRoute());
        ownerReviewArea.setText(historyToEdit.getOwnerReview() == null ? "" : historyToEdit.getOwnerReview());
        walkerReviewArea.setText(historyToEdit.getWalkerReview() == null ? "" : historyToEdit.getWalkerReview());

        if (historyToEdit.getRating() != null) {
            ratingSpinner.getValueFactory().setValue(historyToEdit.getRating());
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();

        try {
            if (historyService == null) {
                showError("Форма не подключена к сервисам.");
                return;
            }

            WalkHistory history = historyToEdit == null ? new WalkHistory() : historyToEdit;

            if (historyToEdit == null) {
                WalkRequestOption option = requestComboBox.getValue();
                if (option == null) {
                    showFieldError(requestComboBox, "Выберите заявку.");
                    return;
                }
                history.setWalkRequestId(option.requestId());
            }

            String durationText = durationField.getText() == null ? "" : durationField.getText().trim();
            if (!durationText.isEmpty()) {
                try {
                    history.setActualDuration(Integer.parseInt(durationText));
                } catch (NumberFormatException e) {
                    showFieldError(durationField, "Длительность должна быть целым числом.");
                    return;
                }
            } else {
                history.setActualDuration(null);
            }

            history.setRoute(routeField.getText());
            history.setOwnerReview(ownerReviewArea.getText());
            history.setWalkerReview(walkerReviewArea.getText());
            history.setRating(ratingSpinner.getValue());

            if (historyToEdit == null) {
                historyService.create(history);
            } else {
                historyService.update(history);
            }

            saved = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } catch (ValidationException e) {
            Control field = switch (e.getField()) {
                case "actualDuration" -> durationField;
                case "rating" -> ratingSpinner;
                case "walkRequest" -> requestComboBox;
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
            showError("Ошибка БД: " + e.getMessage());
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
        for (Control c : List.of(requestComboBox, durationField, routeField, ratingSpinner)) {
            c.getStyleClass().remove("field-error");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}