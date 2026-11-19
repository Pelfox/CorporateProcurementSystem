CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(255),
    event_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID,
    description VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(64)
);

CREATE INDEX audit_events_time_idx ON audit_events (occurred_at DESC, id);
CREATE INDEX audit_events_actor_idx ON audit_events (actor_id, occurred_at DESC);
CREATE INDEX audit_events_actor_name_idx ON audit_events (actor_username);
CREATE INDEX audit_events_type_idx ON audit_events (event_type, occurred_at DESC);
CREATE INDEX audit_events_entity_idx ON audit_events (entity_type, entity_id, occurred_at DESC);
CREATE INDEX audit_events_correlation_idx ON audit_events (correlation_id);

INSERT INTO audit_events (id, occurred_at, actor_id, actor_username, event_type,
                          entity_type, entity_id, description, correlation_id)
SELECT a.id, a.changed_at, a.changed_by, u.username, 'ORDER_STATUS_CHANGED',
       'ORDER', a.order_id,
       'Статус заказа: ' || CAST(a.old_status AS VARCHAR) || ' → ' || CAST(a.new_status AS VARCHAR) || '.',
       NULL
FROM audit_logs a LEFT JOIN users u ON u.id = a.changed_by;
