ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;

UPDATE users SET last_seen_at = updated_at WHERE last_seen_at IS NULL;
