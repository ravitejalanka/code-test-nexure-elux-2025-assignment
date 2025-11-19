CREATE TABLE products (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_price DOUBLE PRECISION NOT NULL,
    country VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE applied_discounts (
    product_id VARCHAR(255) NOT NULL,
    discount_id VARCHAR(255) NOT NULL,
    percent DOUBLE PRECISION NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (product_id, discount_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- CRITICAL: This unique index enforces idempotency!
CREATE UNIQUE INDEX unique_product_discount
    ON applied_discounts(product_id, discount_id);

-- Test data
INSERT INTO products VALUES
    ('prod-1', 'Laptop', 1000.0, 'SE', CURRENT_TIMESTAMP),
    ('prod-2', 'Mouse', 50.0, 'DE', CURRENT_TIMESTAMP),
    ('prod-3', 'Keyboard', 150.0, 'FR', CURRENT_TIMESTAMP);