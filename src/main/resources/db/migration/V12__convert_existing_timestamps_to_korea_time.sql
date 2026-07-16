-- Production originally ran in UTC while timestamp columns stored timezone-less values.
-- Convert those existing wall-clock values once before all new writes use Asia/Seoul.
UPDATE products
SET created_at = created_at + INTERVAL '9 hours',
    updated_at = updated_at + INTERVAL '9 hours';

UPDATE customer_orders
SET created_at = created_at + INTERVAL '9 hours',
    paid_at = CASE WHEN paid_at IS NULL THEN NULL ELSE paid_at + INTERVAL '9 hours' END;

UPDATE members
SET created_at = created_at + INTERVAL '9 hours';

UPDATE sms_verifications
SET expires_at = expires_at + INTERVAL '9 hours',
    verified_at = CASE WHEN verified_at IS NULL THEN NULL ELSE verified_at + INTERVAL '9 hours' END,
    created_at = created_at + INTERVAL '9 hours';

UPDATE social_accounts
SET created_at = created_at + INTERVAL '9 hours';

UPDATE member_admin_actions
SET created_at = created_at + INTERVAL '9 hours';
