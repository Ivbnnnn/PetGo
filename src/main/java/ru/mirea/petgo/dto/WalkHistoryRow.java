package ru.mirea.petgo.dto;

import java.time.LocalDateTime;

public record WalkHistoryRow(
        int id,
        int walkRequestId,
        String petName,
        LocalDateTime walkDateTime,
        Integer actualDuration,
        String route,
        String ownerReview,
        String walkerReview,
        Integer rating,
        LocalDateTime completedAt) {
}