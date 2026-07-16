ALTER TABLE members ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE member_admin_actions (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(30) NOT NULL,
    before_value VARCHAR(200),
    after_value VARCHAR(200),
    reason VARCHAR(200),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_member_admin_action_member ON member_admin_actions(member_id,created_at DESC);
CREATE INDEX idx_member_admin_action_created ON member_admin_actions(created_at DESC);
