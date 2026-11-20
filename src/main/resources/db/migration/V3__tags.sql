-- Теги заявок. Номер V3 подставьте следующий свободный после V1 и V2.
-- Если теги уже созданы в V2, этот файл не добавляйте: подстройте сущность Tag под существующие таблицы.

CREATE TABLE tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL,
    color       VARCHAR(7),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_tags_name_lower ON tags (LOWER(name));

CREATE TABLE walk_request_tags (
    walk_request_id UUID NOT NULL REFERENCES walk_requests (id) ON DELETE CASCADE,
    tag_id          UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (walk_request_id, tag_id)
);

CREATE INDEX idx_walk_request_tags_tag ON walk_request_tags (tag_id);
