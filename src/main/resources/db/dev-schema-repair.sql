-- The local H2 database predates the Toss payment fields. Flyway is intentionally
-- disabled for the default development profile because this database was originally
-- created by Hibernate. Keep this startup patch idempotent so existing data survives.
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'READY';
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS payment_key VARCHAR(200);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(40);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS receipt_url VARCHAR(500);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS payment_failure_code VARCHAR(80);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS payment_failure_message VARCHAR(200);

UPDATE customer_orders
SET payment_status = 'PAID'
WHERE status IN ('PAID', 'PREPARING', 'SHIPPED', 'DELIVERED')
  AND payment_status = 'READY';

UPDATE customer_orders
SET payment_status = 'CANCELLED'
WHERE status = 'CANCELLED'
  AND payment_status <> 'CANCELLED';

CREATE UNIQUE INDEX IF NOT EXISTS idx_order_payment_key ON customer_orders(payment_key);
CREATE INDEX IF NOT EXISTS idx_order_retention_cleanup ON customer_orders(status, payment_status, created_at);
