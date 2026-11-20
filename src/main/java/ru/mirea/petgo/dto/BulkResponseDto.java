package ru.mirea.petgo.dto;

import java.util.List;

public record BulkResponseDto(int total, int succeeded, int failed, List<BulkItemResult> results) {
}
