CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(40) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX idx_member_email ON members(email);

ALTER TABLE customer_orders ADD COLUMN member_id BIGINT REFERENCES members(id);
ALTER TABLE customer_orders ADD COLUMN points_used INTEGER NOT NULL DEFAULT 0 CHECK (points_used >= 0);
ALTER TABLE customer_orders ADD COLUMN points_earned INTEGER NOT NULL DEFAULT 0 CHECK (points_earned >= 0);
CREATE INDEX idx_order_member_created ON customer_orders(member_id, created_at DESC);
