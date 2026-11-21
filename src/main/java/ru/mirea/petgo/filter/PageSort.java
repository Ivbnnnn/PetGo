package ru.mirea.petgo.filter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.mirea.petgo.error.BadRequestException;

import java.util.Set;

public final class PageSort {
    private PageSort() {
    }

    public static Pageable apply(Pageable page, Sort defaultSort, Set<String> allowed) {
        Sort sort = page.getSortOr(defaultSort);
        for (Sort.Order order : sort) {
            if (!allowed.contains(order.getProperty())) {
                throw new BadRequestException("Недопустимое поле сортировки: " + order.getProperty());
            }
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(page.getPageNumber(), page.getPageSize(), sort);
    }
}
