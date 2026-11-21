CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE pets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    weight NUMERIC(5,2) CHECK (weight > 0),
    age INTEGER CHECK (age >= 0),
    special_needs TEXT,
    owner_id UUID NOT NULL REFERENCES users (id),
    photo_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pets_owner_name ON pets (owner_id, lower(name));

CREATE TABLE walk_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(50) UNIQUE,
    pet_id UUID NOT NULL REFERENCES pets (id),
    owner_id UUID NOT NULL REFERENCES users (id),
    walker_id UUID REFERENCES users (id),
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    address VARCHAR(200) NOT NULL,
    description TEXT,
    price DOUBLE PRECISION NOT NULL CHECK (price >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(20) CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_walk_requests_pet ON walk_requests (pet_id);
CREATE INDEX idx_walk_requests_owner ON walk_requests (owner_id);
CREATE INDEX idx_walk_requests_walker ON walk_requests (walker_id);
CREATE INDEX idx_walk_requests_status ON walk_requests (status);
CREATE INDEX idx_walk_requests_archived_scheduled ON walk_requests (is_archived, scheduled_at);

CREATE TABLE walk_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_request_id UUID NOT NULL UNIQUE REFERENCES walk_requests (id) ON DELETE CASCADE,
    actual_duration INTEGER CHECK (actual_duration > 0),
    route TEXT,
    owner_review TEXT,
    walker_review TEXT,
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
