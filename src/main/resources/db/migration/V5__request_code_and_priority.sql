UPDATE walk_requests SET external_id = 'WR-' || gen_random_uuid()::text WHERE external_id IS NULL;
UPDATE walk_requests SET priority = CASE
    WHEN duration_minutes >= 60 THEN 'HIGH'
    WHEN duration_minutes >= 30 THEN 'NORMAL'
    ELSE 'LOW'
END WHERE priority IS NULL;
ALTER TABLE walk_requests ALTER COLUMN external_id SET NOT NULL, ALTER COLUMN priority SET NOT NULL;
