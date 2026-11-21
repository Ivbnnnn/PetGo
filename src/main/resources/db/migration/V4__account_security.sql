ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'OWNER' CHECK (role IN ('OWNER', 'WALKER')),
    ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'VIEWER' CHECK (access_level IN ('VIEWER', 'EDITOR', 'ADMIN')),
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN password_hash VARCHAR(60),
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN credential_version UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_access_level ON users (access_level);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    credential_version UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_sessions_user ON refresh_sessions (user_id);
CREATE INDEX idx_refresh_sessions_expiry ON refresh_sessions (expires_at);
