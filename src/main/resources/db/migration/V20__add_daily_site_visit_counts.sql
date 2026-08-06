CREATE TABLE site_visit_daily (
    visit_date DATE PRIMARY KEY,
    view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0)
);
