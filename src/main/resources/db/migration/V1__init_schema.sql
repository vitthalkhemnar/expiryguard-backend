-- V1__init_schema.sql
-- Schema definition for ExpiryGuard

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL, -- ROLE_ADMIN, ROLE_CASHIER
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    min_reorder_level INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_barcode ON products(barcode);

CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    batch_number VARCHAR(100) NOT NULL UNIQUE,
    initial_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    cost_price NUMERIC(12, 2) NOT NULL,
    base_selling_price NUMERIC(12, 2) NOT NULL,
    current_selling_price NUMERIC(12, 2) NOT NULL,
    discount_percentage INT NOT NULL DEFAULT 0,
    manufacturing_date DATE,
    expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL, -- ACTIVE, NEAR_EXPIRY, CRITICAL, EXPIRED, DISPOSED
    version BIGINT NOT NULL DEFAULT 0, -- For optimistic locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_batches_product_expiry ON batches(product_id, expiry_date);
CREATE INDEX idx_batches_status ON batches(status);

CREATE TABLE markdown_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    days_before_expiry INT NOT NULL UNIQUE,
    discount_percentage INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    cashier_id BIGINT NOT NULL REFERENCES users(id),
    customer_name VARCHAR(100),
    customer_phone VARCHAR(20),
    subtotal NUMERIC(12, 2) NOT NULL,
    total_discount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- CASH, CARD, UPI
    payment_status VARCHAR(30) NOT NULL, -- PAID, REFUNDED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    quantity INT NOT NULL,
    unit_cost_price NUMERIC(12, 2) NOT NULL,
    unit_selling_price NUMERIC(12, 2) NOT NULL,
    discount_applied NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_price NUMERIC(12, 2) NOT NULL
);

CREATE TABLE disposal_logs (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    approved_by_id BIGINT NOT NULL REFERENCES users(id),
    quantity_disposed INT NOT NULL,
    financial_loss_amount NUMERIC(12, 2) NOT NULL,
    reason VARCHAR(50) NOT NULL, -- EXPIRED, DAMAGED, RECALLED
    notes TEXT,
    disposed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
