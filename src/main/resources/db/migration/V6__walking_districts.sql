CREATE TABLE districts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_districts_name ON districts (lower(name));

ALTER TABLE walk_requests ADD COLUMN district_id UUID REFERENCES districts (id);
CREATE INDEX idx_walk_requests_district ON walk_requests (district_id);
