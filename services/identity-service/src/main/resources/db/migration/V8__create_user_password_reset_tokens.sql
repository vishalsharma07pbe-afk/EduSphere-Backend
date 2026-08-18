CREATE TABLE user_password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_password_reset_tokens_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_user_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_password_reset_tokens_user_id
    ON user_password_reset_tokens (user_id);

CREATE INDEX idx_user_password_reset_tokens_active_user
    ON user_password_reset_tokens (user_id)
    WHERE used_at IS NULL AND revoked_at IS NULL;
