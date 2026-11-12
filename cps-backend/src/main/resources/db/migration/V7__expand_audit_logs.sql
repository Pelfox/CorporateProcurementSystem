-- order_id, old_status, new_status nullable
ALTER TABLE audit_logs ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN old_status DROP NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN new_status DROP NOT NULL;

-- Новые колонки
ALTER TABLE audit_logs ADD COLUMN entity_type VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN entity_uuid UUID;
ALTER TABLE audit_logs ADD COLUMN event_type VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN description VARCHAR(500);
ALTER TABLE audit_logs ADD COLUMN correlation_id VARCHAR(64);

-- Миграция существующие записи
UPDATE audit_logs
SET entity_type  = 'ORDER',
    entity_uuid  = order_id,
    event_type   = 'STATUS_CHANGED',
    description  = 'Статус заказа изменён: ' || old_status || ' → ' || new_status
WHERE entity_type IS NULL;

-- Снимаем старые CHECK (они запрещали null)
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_old_status;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_new_status;

-- Новые CHECK
ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_entity_type
    CHECK (entity_type IS NULL OR entity_type IN
                                  ('ORDER', 'PRODUCT', 'CATEGORY', 'WAREHOUSE', 'INVENTORY', 'USER', 'ORDER_ITEM'));

ALTER TABLE audit_logs ADD CONSTRAINT ck_audit_logs_event_type
    CHECK (event_type IS NULL OR event_type IN
                                 ('LOGIN', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGED', 'PASSWORD_RESET',
                                  'CREATED', 'UPDATED', 'DELETED', 'PERMANENTLY_DELETED',
                                  'ARCHIVED', 'RESTORED', 'STATUS_CHANGED',
                                  'BULK_ARCHIVED', 'BULK_RESTORED', 'BULK_STATUS_CHANGED',
                                  'BULK_TAG_ADDED', 'BULK_TAG_REMOVED',
                                  'USER_CREATED', 'USER_UPDATED', 'USER_DISABLED', 'REFERENCE_UPDATED'));

-- Индексы
CREATE INDEX idx_audit_logs_entity_type   ON audit_logs (entity_type);
CREATE INDEX idx_audit_logs_entity_uuid   ON audit_logs (entity_uuid);
CREATE INDEX idx_audit_logs_event_type    ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_changed_at    ON audit_logs (changed_at DESC);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs (correlation_id);