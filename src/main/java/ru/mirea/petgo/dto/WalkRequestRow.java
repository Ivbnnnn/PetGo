package ru.mirea.petgo.dto;

import ru.mirea.petgo.model.enums.WalkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalkRequestRow(
        int id,
        int petId, String petName,
        int ownerId, String ownerName,
        Integer walkerId, String walkerName,
        LocalDateTime walkDateTime,
        int durationMinutes,
        String walkAddress,
        WalkStatus status,
        String description,
        BigDecimal price) {
}
