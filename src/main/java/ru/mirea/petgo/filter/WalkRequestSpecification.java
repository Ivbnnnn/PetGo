package ru.mirea.petgo.filter;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.petgo.domain.WalkRequest;
import ru.mirea.petgo.dto.WalkRequestFilterDto;

import java.util.ArrayList;
import java.util.List;

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
}