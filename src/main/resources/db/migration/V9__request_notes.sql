
CREATE TABLE IF NOT EXISTS request_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    walk_request_id UUID NOT NULL
        REFERENCES walk_requests(id)
        ON DELETE CASCADE,

    author_id UUID NOT NULL
        REFERENCES users(id),

    content TEXT NOT NULL
        CHECK (length(trim(content)) > 0),

    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_request_notes_request
    ON request_notes(walk_request_id);

CREATE INDEX IF NOT EXISTS idx_request_notes_author
    ON request_notes(author_id);

CREATE INDEX IF NOT EXISTS idx_request_notes_order
    ON request_notes(
        walk_request_id,
        is_pinned DESC,
        created_at DESC
    );
