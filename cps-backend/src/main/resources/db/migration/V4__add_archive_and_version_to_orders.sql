-- Внешний код заказа
ALTER TABLE orders ADD COLUMN external_code VARCHAR(50);
CREATE UNIQUE INDEX uk_orders_external_code ON orders (external_code);
-- Признак архива
ALTER TABLE orders ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN archived_at TIMESTAMP(6) WITH TIME ZONE;

-- Поле version для оптимистичного блокирования
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Индекс для фильтрации активных/архивных
CREATE INDEX idx_orders_archived ON orders (archived);