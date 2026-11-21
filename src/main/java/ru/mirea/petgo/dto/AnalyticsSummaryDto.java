package ru.mirea.petgo.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsSummaryDto(
        LocalDate from, LocalDate to,
        long total, long active, long archived, long inPeriod,
        Map<String, Long> byStatus, Map<String, Long> byPriority,
        Double avgPrice, Double minPrice, Double maxPrice,
        Double revenueCompleted, Double averageRating,
        List<NameCountDto> topPets, List<NameCountDto> topOwners, List<NameCountDto> topWalkers,
        List<NameCountDto> byDistrict) {
}
