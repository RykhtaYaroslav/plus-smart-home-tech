CREATE TABLE IF NOT EXISTS inventory_items (
    product_id BIGINT PRIMARY KEY,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT inventory_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT inventory_reserved_quantity_nonnegative CHECK (reserved_quantity >= 0),
    CONSTRAINT inventory_reserved_quantity_within_stock CHECK (reserved_quantity <= quantity)
);
