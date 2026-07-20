CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_review_product_member UNIQUE (product_id, member_id)
);
CREATE INDEX idx_review_product_created ON product_reviews(product_id, created_at DESC);

CREATE TABLE product_inquiries (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    answer VARCHAR(3000),
    answered_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL,
    answered_at TIMESTAMP
);
CREATE INDEX idx_inquiry_product_created ON product_inquiries(product_id, created_at DESC);
CREATE INDEX idx_inquiry_answered_created ON product_inquiries(answered_at, created_at DESC);

CREATE TABLE notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(10000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_notice_pinned_created ON notices(pinned DESC, created_at DESC);
