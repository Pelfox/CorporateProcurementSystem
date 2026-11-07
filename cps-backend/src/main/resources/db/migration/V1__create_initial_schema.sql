CREATE TABLE users (
    id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    user_role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (user_role IN ('USER', 'MANAGER'))
);

CREATE TABLE categories (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE warehouses (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    CONSTRAINT pk_warehouses PRIMARY KEY (id)
);

CREATE TABLE products (
    id UUID NOT NULL,
    sku VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category_id UUID,
    price NUMERIC(10, 2) NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE inventory (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT uk_inventory_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
);

CREATE TABLE orders (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_status VARCHAR(255) NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_orders_status CHECK (order_status IN ('CREATED', 'APPROVED', 'DELIVERED', 'CANCELLED'))
);

CREATE TABLE order_items (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    purchase_price NUMERIC(10, 2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE audit_logs (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    old_status VARCHAR(255) NOT NULL,
    new_status VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_audit_logs_changed_by FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_audit_logs_old_status CHECK (old_status IN ('CREATED', 'APPROVED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_audit_logs_new_status CHECK (new_status IN ('CREATED', 'APPROVED', 'DELIVERED', 'CANCELLED'))
);
