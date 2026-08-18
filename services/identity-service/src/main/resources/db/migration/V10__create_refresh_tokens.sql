CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    token_family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens (token_family_id);

CREATE INDEX idx_refresh_tokens_active_user
    ON refresh_tokens (user_id)
    WHERE revoked_at IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN version DROP DEFAULT;