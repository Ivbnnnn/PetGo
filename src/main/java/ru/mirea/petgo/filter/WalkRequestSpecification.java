package ru.mirea.petgo.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.WalkRequestFilterDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WalkRequestSpecification {

    public static Specification<WalkRequest> getFiltered(WalkRequestFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getIsArchived() != null) {
                predicates.add(cb.equal(root.get("isArchived"), filter.getIsArchived()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), filter.getDateTo()));
            }
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchLike = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("address")), searchLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<WalkRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<WalkRequest> isArchived(Boolean archived) {
        return (root, query, cb) -> archived == null ? null : cb.equal(root.get("isArchived"), archived);
    }

    public static Specification<WalkRequest> priceGreaterThan(Double price) {
        return (root, query, cb) -> price == null ? null : cb.greaterThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<WalkRequest> priceLessThan(Double price) {
        return (root, query, cb) -> price == null ? null : cb.lessThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<WalkRequest> scheduledAfter(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get("scheduledAt"), date);
    }

    public static Specification<WalkRequest> scheduledBefore(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get("scheduledAt"), date);
    }

    public static Specification<WalkRequest> containsAddress(String address) {
        return (root, query, cb) -> address == null || address.isEmpty() ? null
                : cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%");
    }

    public static Specification<WalkRequest> hasTag(UUID tagId) {
        return (root, query, cb) -> {
            if (tagId == null)
                return null;
            Join<WalkRequest, Tag> tags = root.join("tags", JoinType.INNER);
            return cb.equal(tags.get("id"), tagId);
        };
    }
}