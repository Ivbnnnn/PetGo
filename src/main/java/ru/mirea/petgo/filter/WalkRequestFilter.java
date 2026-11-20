package ru.mirea.petgo.filter;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.WalkRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Общие параметры фильтра: используйте этот же класс в списке заявок и в экспорте,
 * тогда CSV содержит ровно то, что видно на экране. Поиск по питомцу, владельцу и выгульщику
 * добавляет автор списка заявок (новые методы в WalkRequestSpecification и строки в toSpecification).
 */
public record WalkRequestFilter(
        RequestStatus status,
        Boolean archived,
        UUID tagId,
        String address,
        Double priceFrom,
        Double priceTo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTo) {

    public Specification<WalkRequest> toSpecification() {
        return Specification.where(WalkRequestSpecification.hasStatus(status))
                .and(WalkRequestSpecification.isArchived(archived))
                .and(WalkRequestSpecification.hasTag(tagId))
                .and(WalkRequestSpecification.containsAddress(address))
                .and(WalkRequestSpecification.priceGreaterThan(priceFrom))
                .and(WalkRequestSpecification.priceLessThan(priceTo))
                .and(WalkRequestSpecification.scheduledAfter(scheduledFrom))
                .and(WalkRequestSpecification.scheduledBefore(scheduledTo));
    }
}
