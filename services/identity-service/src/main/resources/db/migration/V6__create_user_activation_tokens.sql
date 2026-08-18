CREATE TABLE user_activation_tokens (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_user_activation_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_user_activation_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
);

CREATE INDEX idx_user_activation_tokens_user_id
    ON user_activation_tokens (user_id);

ALTER TABLE user_activation_tokens
    ALTER COLUMN version DROP DEFAULT;