package ru.mirea.petgo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalkRequestUpdateDto {
    @NotNull(message = "Версия обязательна для проверки конфликтов")
    private Long version;

    private UUID walkerId;
    private UUID districtId;
    private LocalDateTime scheduledAt;
    
    @Min(value = 1, message = "Длительность должна быть больше 0")
    private Integer durationMinutes;

    @Size(max = 200, message = "Адрес не должен превышать 200 символов")
    private String address;

    private String description;

    @DecimalMin(value = "0.0", message = "Цена не может быть отрицательной")
    private Double price;
}
