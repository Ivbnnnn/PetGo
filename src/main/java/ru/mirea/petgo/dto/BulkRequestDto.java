package ru.mirea.petgo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.petgo.domain.RequestStatus;

import java.util.List;
import java.util.UUID;

public record BulkRequestDto(
        @NotEmpty(message = "Список заявок пуст")
        @Size(max = 500, message = "За один раз не больше 500 заявок")
        List<@NotNull(message = "ID заявки не может быть пустым") UUID> ids,

        @NotNull(message = "Укажите операцию")
        BulkAction action,

        RequestStatus status,   // для CHANGE_STATUS

        UUID tagId) {           // для ADD_TAG и REMOVE_TAG
}
