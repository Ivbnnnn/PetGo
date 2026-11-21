
package ru.mirea.petgo.filter;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.*;
import ru.mirea.petgo.dto.WalkRequestFilterDto;
import ru.mirea.petgo.error.BadRequestException;

import java.time.LocalDateTime;
import java.util.UUID;

public final class WalkRequestSpecification {

    private WalkRequestSpecification() {
    }

    public static Specification<WalkRequest> getFiltered(
            WalkRequestFilterDto f
    ) {
        if (f == null) {
            f = new WalkRequestFilterDto();
        }

        validate(f);

        return Specification.allOf(
                search(f.getSearch()),
                address(f.getAddress()),
                byId("pet", f.getPetId()),
                byId("owner", f.getOwnerId()),
                byId("walker", f.getWalkerId()),
                byId("district", f.getDistrictId()),
                status(f.getStatus()),
                priority(f.getPriority()),
                priorityFlag(f.getIsPriority()),
                archived(prefer(f.getIsArchived(), f.getArchived())),
                tag(f.getTagId()),
                minPrice(prefer(f.getMinPrice(), f.getPriceFrom())),
                maxPrice(prefer(f.getMaxPrice(), f.getPriceTo())),
                comparePrice(f.getPriceOperation(), f.getPriceValue()),
                from(prefer(f.getDateFrom(), f.getScheduledFrom())),
                to(prefer(f.getDateTo(), f.getScheduledTo()))
        );
    }

    private static <T> T prefer(T first, T second) {
        return first != null ? first : second;
    }

    private static void validate(WalkRequestFilterDto f) {
        Double min = prefer(f.getMinPrice(), f.getPriceFrom());
        Double max = prefer(f.getMaxPrice(), f.getPriceTo());

        for (Double price : new Double[]{
                min, max, f.getPriceValue()
        }) {
            if (price != null &&
                    (!Double.isFinite(price) || price < 0)) {
                throw new BadRequestException(
                        "Цена должна быть конечным неотрицательным числом"
                );
            }
        }

        if (min != null && max != null && min > max) {
            throw new BadRequestException(
                    "Минимальная цена больше максимальной"
            );
        }

        LocalDateTime start = prefer(
                f.getDateFrom(), f.getScheduledFrom()
        );
        LocalDateTime end = prefer(
                f.getDateTo(), f.getScheduledTo()
        );

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException(
                    "Начало периода позже окончания"
            );
        }

        if ((f.getPriceOperation() == null)
                != (f.getPriceValue() == null)) {
            throw new BadRequestException(
                    "Для сравнения цены нужны priceOperation и priceValue"
            );
        }
    }

    private static Specification<WalkRequest> byId(
            String relation, UUID id
    ) {
        return (root, query, cb) ->
                id == null ? null :
                cb.equal(root.get(relation).get("id"), id);
    }

    private static Specification<WalkRequest> status(
            RequestStatus value
    ) {
        return (root, query, cb) ->
                value == null ? null :
                cb.equal(root.get("status"), value);
    }

    private static Specification<WalkRequest> priority(
            RequestPriority value
    ) {
        return (root, query, cb) ->
                value == null ? null :
                cb.equal(root.get("priority"), value);
    }

    // В текущей схеме нет отдельного is_priority.
    // Временная семантика: HIGH означает приоритетную заявку.
    private static Specification<WalkRequest> priorityFlag(
            Boolean value
    ) {
        return (root, query, cb) -> {
            if (value == null) return null;

            return value
                    ? cb.equal(root.get("priority"), RequestPriority.HIGH)
                    : cb.notEqual(root.get("priority"), RequestPriority.HIGH);
        };
    }

    private static Specification<WalkRequest> archived(
            Boolean value
    ) {
        return (root, query, cb) ->
                value == null ? null :
                cb.equal(root.get("isArchived"), value);
    }

    private static Specification<WalkRequest> tag(UUID id) {
        return (root, query, cb) -> {
            if (id == null) return null;

            return cb.equal(
                    root.join("tags", JoinType.INNER).get("id"),
                    id
            );
        };
    }

    private static Specification<WalkRequest> search(
            String value
    ) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return null;

            String pattern = LikePattern.contains(value);

            return cb.or(
                    cb.like(cb.lower(root.get("externalId")),
                            pattern, '!'),
                    cb.like(cb.lower(root.get("address")),
                            pattern, '!'),
                    cb.like(cb.lower(
                            root.join("pet", JoinType.LEFT).get("name")),
                            pattern, '!'),
                    cb.like(cb.lower(
                            root.join("owner", JoinType.LEFT).get("username")),
                            pattern, '!'),
                    cb.like(cb.lower(
                            root.join("walker", JoinType.LEFT).get("username")),
                            pattern, '!')
            );
        };
    }

    private static Specification<WalkRequest> address(
            String value
    ) {
        return (root, query, cb) ->
                value == null || value.isBlank() ? null :
                cb.like(cb.lower(root.get("address")),
                        LikePattern.contains(value), '!');
    }

    private static Specification<WalkRequest> minPrice(Double price) {
        return (root, query, cb) -> price == null ? null :
                cb.ge(root.get("price"), price);
    }

    private static Specification<WalkRequest> maxPrice(Double price) {
        return (root, query, cb) -> price == null ? null :
                cb.le(root.get("price"), price);
    }

    private static Specification<WalkRequest> comparePrice(
            String operation, Double value
    ) {
        return (root, query, cb) -> {
            if (operation == null) return null;

            return switch (operation.toUpperCase(
                    java.util.Locale.ROOT)) {
                case "EQ" ->
                        cb.equal(root.get("price"), value);
                case "GT" ->
                        cb.gt(root.get("price"), value);
                case "LT" ->
                        cb.lt(root.get("price"), value);
                case "GTE" ->
                        cb.ge(root.get("price"), value);
                case "LTE" ->
                        cb.le(root.get("price"), value);
                default -> throw new BadRequestException(
                        "Недопустимая операция сравнения цены"
                );
            };
        };
    }

    private static Specification<WalkRequest> from(
            LocalDateTime date
    ) {
        return (root, query, cb) -> date == null ? null :
                cb.greaterThanOrEqualTo(
                        root.get("scheduledAt"), date
                );
    }

    private static Specification<WalkRequest> to(
            LocalDateTime date
    ) {
        return (root, query, cb) -> date == null ? null :
                cb.lessThanOrEqualTo(
                        root.get("scheduledAt"), date
                );
    }
}
