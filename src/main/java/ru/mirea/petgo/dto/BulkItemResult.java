package ru.mirea.petgo.dto;

import java.util.UUID;

public record BulkItemResult(UUID id, boolean success, String error) {
}
