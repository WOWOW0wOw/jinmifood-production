UPDATE products
SET name = '먹태 39~41cm 1kg 내외',
    summary = '노릇하게 구워 고소한 먹태',
    description = '술안주와 간식으로 좋은 큼직한 먹태입니다.'
WHERE slug = 'meoktae-1kg'
  AND (name LIKE '%ë%' OR summary LIKE '%ë%' OR description LIKE '%ë%');

ALTER TABLE customer_orders ALTER COLUMN subtotal TYPE BIGINT;
ALTER TABLE customer_orders ALTER COLUMN shipping_fee TYPE BIGINT;
ALTER TABLE customer_orders ALTER COLUMN total_amount TYPE BIGINT;
ALTER TABLE order_items ALTER COLUMN unit_price TYPE BIGINT;
ALTER TABLE order_items ALTER COLUMN line_total TYPE BIGINT;
