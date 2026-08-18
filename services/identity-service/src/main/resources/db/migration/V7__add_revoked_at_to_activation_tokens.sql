ALTER TABLE user_activation_tokens
    ADD COLUMN revoked_at TIMESTAMPTZ;

CREATE INDEX idx_user_activation_tokens_active_user
    ON user_activation_tokens (user_id)
    WHERE used_at IS NULL
        AND revoked_at IS NULL;