DROP TABLE IF EXISTS walk_history CASCADE;
DROP TABLE IF EXISTS walk_requests CASCADE;
DROP TABLE IF EXISTS pets CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'WALKER')),
    address     VARCHAR(200),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pets (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    breed          VARCHAR(100),
    weight         DECIMAL(5, 2) CHECK (weight > 0),
    age            INT CHECK (age >= 0),
    special_needs  TEXT,
    owner_id       INT NOT NULL,
    photo_url      VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE walk_requests (
    id                SERIAL PRIMARY KEY,
    pet_id            INT NOT NULL,
    owner_id          INT NOT NULL,
    walker_id         INT,
    walk_datetime     TIMESTAMP NOT NULL,
    duration_minutes  INT NOT NULL CHECK (duration_minutes > 0),
    walk_address      VARCHAR(200) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED','PENDING','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')),
    description       TEXT,
    price             DECIMAL(10, 2) CHECK (price >= 0),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wr_pet    FOREIGN KEY (pet_id)    REFERENCES pets(id),
    CONSTRAINT fk_wr_owner  FOREIGN KEY (owner_id)  REFERENCES users(id),
    CONSTRAINT fk_wr_walker FOREIGN KEY (walker_id) REFERENCES users(id)
);

CREATE TABLE walk_history (
    id                SERIAL PRIMARY KEY,
    walk_request_id   INT NOT NULL UNIQUE,
    actual_duration   INT CHECK (actual_duration > 0),
    route             TEXT,
    owner_review      TEXT,
    walker_review     TEXT,
    rating            INT CHECK (rating BETWEEN 1 AND 5),
    completed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wh_request FOREIGN KEY (walk_request_id) REFERENCES walk_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_role    ON users(role);
CREATE INDEX idx_pets_owner    ON pets(owner_id);
CREATE INDEX idx_wr_status     ON walk_requests(status);
CREATE INDEX idx_wr_owner      ON walk_requests(owner_id);
CREATE INDEX idx_wr_walker     ON walk_requests(walker_id);
CREATE INDEX idx_wr_datetime   ON walk_requests(walk_datetime);