package ru.mirea.petgo.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.petgo.domain.AuditEvent;
import ru.mirea.petgo.domain.AuditEvent.EntityType;
import ru.mirea.petgo.dto.AuditDto;
import ru.mirea.petgo.error.BadRequestException;
import ru.mirea.petgo.filter.CorrelationIdFilter;
import ru.mirea.petgo.filter.LikePattern;
import ru.mirea.petgo.filter.PageSort;
import ru.mirea.petgo.repository.AuditEventRepository;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "occurredAt", "action", "entityType", "entityId", "actorId", "correlationId");

    private final AuditEventRepository events;

    @Transactional
    public void record(String action, EntityType type, UUID entityId, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actor = authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                ? UUID.fromString(jwt.getSubject()) : null;
        recordAs(actor, action, type, entityId, description);
    }

    @Transactional
    public void recordAs(UUID actorId, String action, EntityType type, UUID entityId, String description) {
        events.save(new AuditEvent(actorId, action, type, entityId, MDC.get(CorrelationIdFilter.MDC_KEY), description));
    }

    @Transactional(readOnly = true)
    public Page<AuditDto> search(String text, EntityType type, UUID actor, Pageable page) {
        if (text.length() > 200) {
            throw new BadRequestException("Строка поиска не должна превышать 200 символов");
        }
        Specification<AuditEvent> filters = Specification.allOf(matches(text),
                (root, query, cb) -> type == null ? null : cb.equal(root.get("entityType"), type),
                (root, query, cb) -> actor == null ? null : cb.equal(root.get("actorId"), actor));
        return events.findAll(filters, PageSort.apply(page, Sort.by(Sort.Order.desc("occurredAt")), SORT_FIELDS))
                .map(AuditDto::from);
    }

    private static Specification<AuditEvent> matches(String text) {
        if (text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = LikePattern.contains(text);
        UUID id = parseId(text.strip());
        return (root, query, cb) -> {
            var inText = cb.or(cb.like(cb.lower(root.get("action")), pattern, '!'),
                    cb.like(cb.lower(root.get("description")), pattern, '!'),
                    cb.like(cb.lower(root.get("correlationId")), pattern, '!'));
            return id == null ? inText : cb.or(inText, cb.equal(root.get("id"), id),
                    cb.equal(root.get("entityId"), id), cb.equal(root.get("actorId"), id));
        };
    }

    private static UUID parseId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
