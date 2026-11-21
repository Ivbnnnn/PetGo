package ru.mirea.petgo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalkRequestCreateDto {
    @NotNull(message = "ID питомца обязателен")
    private UUID petId;

    @NotNull(message = "ID владельца обязателен")
    private UUID ownerId;

    private UUID walkerId;
    private UUID districtId;

    @NotNull(message = "Дата прогулки обязательна")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Длительность обязательна")
    @Min(value = 1, message = "Длительность должна быть больше 0")
    private Integer durationMinutes;

    @NotBlank(message = "Адрес обязателен")
    @Size(max = 200, message = "Адрес не должен превышать 200 символов")
    private String address;

    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    private Double price;
}
