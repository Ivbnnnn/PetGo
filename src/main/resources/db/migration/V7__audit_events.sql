CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(64),
    description VARCHAR(500) NOT NULL
);

CREATE INDEX idx_audit_events_time ON audit_events (occurred_at DESC, id);
CREATE INDEX idx_audit_events_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_events_actor ON audit_events (actor_id);
CREATE INDEX idx_audit_events_correlation ON audit_events (correlation_id);
