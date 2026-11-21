
package ru.mirea.petgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestNoteDto(
        UUID id,
        UUID requestId,
        UUID authorId,
        @NotBlank(message = "Текст заметки обязателен")
        @Size(max = 5000)
        String content,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
