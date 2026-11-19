ALTER TABLE products
    DROP CONSTRAINT products_price_check,
    ADD CONSTRAINT ck_products_price_nonnegative CHECK (price >= 0);

ALTER TABLE order_items
    DROP CONSTRAINT order_items_purchase_price_check,
    ADD CONSTRAINT ck_order_items_purchase_price_nonnegative CHECK (purchase_price >= 0);

ALTER TABLE inventory
    DROP CONSTRAINT inventory_quantity_check,
    ADD CONSTRAINT ck_inventory_quantity_nonnegative CHECK (quantity >= 0);

ALTER TABLE inventory
    ADD CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_id, warehouse_id);
