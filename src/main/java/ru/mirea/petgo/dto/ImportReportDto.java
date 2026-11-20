package ru.mirea.petgo.dto;

import java.util.List;

public record ImportReportDto(
        int totalRows, int validRows, int errorRows,
        int toCreate, int toUpdate, int skipped,
        boolean applied, List<ImportRowError> errors) {
}
