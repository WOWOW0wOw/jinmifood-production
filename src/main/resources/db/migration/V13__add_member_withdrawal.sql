ALTER TABLE members ADD COLUMN withdrawn_at TIMESTAMP;
CREATE INDEX idx_member_withdrawn_at ON members(withdrawn_at);
