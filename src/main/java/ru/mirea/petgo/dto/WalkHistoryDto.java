
package ru.mirea.petgo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalkHistoryDto(
        UUID id,
        UUID requestId,
        @NotNull
        @Min(value = 1, message = "Длительность должна быть больше нуля")
        Integer actualDuration,
        String route,
        String ownerReview,
        String walkerReview,
        @Min(1) @Max(5) Integer rating,
        LocalDateTime completedAt
) {
}
