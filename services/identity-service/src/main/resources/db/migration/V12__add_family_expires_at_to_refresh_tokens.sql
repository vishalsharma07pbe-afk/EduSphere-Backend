ALTER TABLE refresh_tokens
    ADD COLUMN family_expires_at TIMESTAMPTZ;

UPDATE refresh_tokens
SET family_expires_at = created_at + INTERVAL '90 days'
WHERE family_expires_at IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN family_expires_at SET NOT NULL;
