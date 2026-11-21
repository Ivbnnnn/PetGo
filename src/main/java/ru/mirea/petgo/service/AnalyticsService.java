package ru.mirea.petgo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.RequestPriority;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.dto.AnalyticsSummaryDto;
import ru.mirea.petgo.dto.NameCountDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.repository.AnalyticsRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final int TOP_LIMIT = 10;

    private final AnalyticsRepository repo;

    public AnalyticsService(AnalyticsRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto summary(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("Дата «по» раньше даты «с»");
        }
        // даты передаются всегда: null в условии на PostgreSQL с Hibernate 6 даёт ошибку типа параметра
        LocalDateTime f = from == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : from.atStartOfDay();
        LocalDateTime t = to == null ? LocalDateTime.of(2999, 1, 1, 0, 0) : to.plusDays(1).atStartOfDay();
        Pageable top = PageRequest.of(0, TOP_LIMIT);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        repo.countByStatus(f, t).forEach(x -> byStatus.put(x.getStatus().name(), x.getCnt()));

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (RequestPriority p : RequestPriority.values()) {
            byPriority.put(p.name(), 0L);
        }
        repo.countByPriority(f, t).forEach(x ->
                byPriority.merge(x.getPriority() == null ? "NOT_SET" : x.getPriority().name(), x.getCnt(), Long::sum));

        AnalyticsRepository.PriceStats price = repo.priceStats(f, t);
        long total = repo.countAll();
        long archived = repo.countArchived();

        return new AnalyticsSummaryDto(
                from, to,
                total, total - archived, archived,
                price.getCnt() == null ? 0L : price.getCnt(),
                byStatus, byPriority,
                round(price.getAvgPrice()), round(price.getMinPrice()), round(price.getMaxPrice()),
                round(repo.revenueCompleted(f, t)), round(repo.averageRating()),
                toDto(repo.topPets(f, t, top)),
                toDto(repo.topOwners(f, t, top)),
                toDto(repo.topWalkers(f, t, top)),
                toDto(repo.countByDistrict(f, t)));
    }

    private static List<NameCountDto> toDto(List<AnalyticsRepository.NameCount> rows) {
        return rows.stream().map(r -> new NameCountDto(r.getName(), r.getCnt())).toList();
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
