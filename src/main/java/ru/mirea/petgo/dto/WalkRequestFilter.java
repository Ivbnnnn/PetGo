package ru.mirea.petgo.dto;

import ru.mirea.petgo.model.enums.WalkStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WalkRequestFilter(
        String search,
        WalkStatus status,
        Integer walkerId,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        String sortBy,
        boolean ascending
) {
    public WalkRequestFilter {
        search = search == null ? "" : search.trim();
        sortBy = sortBy == null ? "date" : sortBy;
    }

    public static WalkRequestFilter empty() {
        return new WalkRequestFilter(
                "", null, null,
                null, null, null, null,
                "date", false
        );
    }
}
