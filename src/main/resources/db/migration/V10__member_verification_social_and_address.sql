ALTER TABLE members ADD COLUMN postal_code VARCHAR(10);
ALTER TABLE members ADD COLUMN address VARCHAR(200);
ALTER TABLE members ADD COLUMN address_detail VARCHAR(200);

-- Keep legacy accounts compatible with the normalized phone format used by
-- SMS verification and account recovery.
UPDATE members SET phone = REGEXP_REPLACE(phone, '[^0-9]', '', 'g');

CREATE TABLE sms_verifications (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_sms_phone_purpose_created ON sms_verifications(phone,purpose,created_at DESC);
CREATE INDEX idx_sms_expires ON sms_verifications(expires_at);

CREATE TABLE social_accounts (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_social_provider_user UNIQUE(provider,provider_user_id)
);
CREATE INDEX idx_social_member ON social_accounts(member_id);
