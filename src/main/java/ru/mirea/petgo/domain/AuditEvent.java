package ru.mirea.petgo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {
    public enum EntityType {
        USER, WALK_REQUEST, PET, DISTRICT, TAG
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntityType entityType;

    private UUID entityId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = 64)
    private String correlationId;

    @Column(nullable = false, length = 500)
    private String description;

    public AuditEvent(UUID actorId, String action, EntityType entityType,
                      UUID entityId, String correlationId, String description) {
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.occurredAt = Instant.now();
        this.correlationId = correlationId;
        this.description = description;
    }
}
