CREATE INDEX idx_products_category_id ON products (category_id);
-- The inventory unique constraint already indexes (product_id, warehouse_id).
CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_id);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
CREATE INDEX idx_audit_logs_order_id ON audit_logs (order_id);
CREATE INDEX idx_audit_logs_changed_by ON audit_logs (changed_by);
