package ru.mirea.petgo.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.RequestPriority;
import ru.mirea.petgo.domain.RequestStatus;
import ru.mirea.petgo.domain.Tag;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.WalkRequestFilterDto;

import java.time.LocalDateTime;
import java.util.UUID;

public class WalkRequestSpecification {

    public static Specification<WalkRequest> getFiltered(WalkRequestFilterDto filter) {
        return Specification.allOf(hasStatus(filter.getStatus()), hasPriority(filter.getPriority()),
                isArchived(prefer(filter.getIsArchived(), filter.getArchived())), hasTag(filter.getTagId()),
                hasDistrict(filter.getDistrictId()),
                containsAddress(filter.getAddress()), containsSearch(filter.getSearch()),
                priceGreaterThan(prefer(filter.getMinPrice(), filter.getPriceFrom())),
                priceLessThan(prefer(filter.getMaxPrice(), filter.getPriceTo())),
                scheduledAfter(prefer(filter.getDateFrom(), filter.getScheduledFrom())),
                scheduledBefore(prefer(filter.getDateTo(), filter.getScheduledTo())));
    }

    private static <T> T prefer(T value, T alias) {
        return value != null ? value : alias;
    }

    public static Specification<WalkRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<WalkRequest> hasPriority(RequestPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    private static Specification<WalkRequest> hasDistrict(UUID id) {
        return (root, query, cb) -> id == null ? null : cb.equal(root.get("district").get("id"), id);
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
        return (root, query, cb) -> address == null || address.isBlank() ? null
                : cb.like(cb.lower(root.get("address")), LikePattern.contains(address), '!');
    }

    private static Specification<WalkRequest> containsSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = LikePattern.contains(search);
            return cb.or(cb.like(cb.lower(root.get("externalId")), pattern, '!'),
                    cb.like(cb.lower(root.get("address")), pattern, '!'),
                    cb.like(cb.lower(root.get("description")), pattern, '!'),
                    cb.like(cb.lower(root.join("pet", JoinType.LEFT).get("name")), pattern, '!'),
                    cb.like(cb.lower(root.join("owner", JoinType.LEFT).get("username")), pattern, '!'),
                    cb.like(cb.lower(root.join("walker", JoinType.LEFT).get("username")), pattern, '!'));
        };
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
