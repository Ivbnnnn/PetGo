package ru.mirea.petgo.filter;

import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.domain.RequestStatus;

import java.time.LocalDateTime;

public class WalkRequestSpecification {

    public static Specification<WalkRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<WalkRequest> isArchived(Boolean isArchived) {
        return (root, query, cb) -> isArchived == null ? null : cb.equal(root.get("isArchived"), isArchived);
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
        return (root, query, cb) -> address == null || address.isEmpty() ? null : 
                cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%");
    }
}