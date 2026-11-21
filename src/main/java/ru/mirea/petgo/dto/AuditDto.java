package ru.mirea.petgo.dto;

import ru.mirea.petgo.domain.AuditEvent;
import ru.mirea.petgo.domain.AuditEvent.EntityType;

import java.time.Instant;
import java.util.UUID;

public record AuditDto(UUID id, UUID actorId, String action, EntityType entityType, UUID entityId,
                       Instant occurredAt, String correlationId, String description) {
    public static AuditDto from(AuditEvent event) {
        return new AuditDto(event.getId(), event.getActorId(), event.getAction(), event.getEntityType(),
                event.getEntityId(), event.getOccurredAt(), event.getCorrelationId(), event.getDescription());
    }
}
