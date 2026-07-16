ALTER TABLE customer_orders ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'READY';
ALTER TABLE customer_orders ADD COLUMN payment_key VARCHAR(200);
ALTER TABLE customer_orders ADD COLUMN payment_method VARCHAR(40);
ALTER TABLE customer_orders ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE customer_orders ADD COLUMN receipt_url VARCHAR(500);
ALTER TABLE customer_orders ADD COLUMN payment_failure_code VARCHAR(80);
ALTER TABLE customer_orders ADD COLUMN payment_failure_message VARCHAR(200);

UPDATE customer_orders SET payment_status='PAID'
WHERE status IN ('PAID','PREPARING','SHIPPED','DELIVERED');
UPDATE customer_orders SET payment_status='CANCELLED'
WHERE status='CANCELLED';

CREATE UNIQUE INDEX idx_order_payment_key ON customer_orders(payment_key);
