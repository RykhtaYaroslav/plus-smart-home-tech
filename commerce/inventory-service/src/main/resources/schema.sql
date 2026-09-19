CREATE TABLE IF NOT EXISTS inventory_items (
    product_id BIGINT PRIMARY KEY,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);