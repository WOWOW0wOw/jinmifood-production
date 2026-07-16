CREATE INDEX IF NOT EXISTS idx_order_retention_cleanup
ON customer_orders(status, payment_status, created_at);
