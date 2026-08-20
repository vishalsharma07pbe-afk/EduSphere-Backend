CREATE TABLE organization_provisioning_requests (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(200) NOT NULL,
    organization_id BIGINT NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    error_summary VARCHAR(500),
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_org_provisioning_idempotency_key
        UNIQUE (idempotency_key),
    CONSTRAINT ck_org_provisioning_organization_id_positive
        CHECK (organization_id > 0),
    CONSTRAINT ck_org_provisioning_idempotency_key_not_blank
        CHECK (BTRIM(idempotency_key) <> ''),
    CONSTRAINT ck_org_provisioning_request_hash
        CHECK (
            request_hash ~ '^[0-9a-f]{64}$'
            ),
    CONSTRAINT ck_org_provisioning_status
        CHECK (
            status IN (
                       'PROCESSING',
                       'SUCCEEDED',
                       'FAILED'
                )
            ),
    CONSTRAINT ck_org_provisioning_state
        CHECK (
            (
                status = 'PROCESSING'
                    AND response_payload IS NULL
                    AND error_summary IS NULL
                    AND completed_at IS NULL
                )
                OR
            (
                status = 'SUCCEEDED'
                    AND response_payload IS NOT NULL
                    AND BTRIM(response_payload) <> ''
                    AND error_summary IS NULL
                    AND completed_at IS NOT NULL
                )
                OR
            (
                status = 'FAILED'
                    AND response_payload IS NULL
                    AND error_summary IS NOT NULL
                    AND BTRIM(error_summary) <> ''
                    AND completed_at IS NOT NULL
                )
            )
);

CREATE INDEX idx_org_provisioning_organization_id
    ON organization_provisioning_requests (
       organization_id
);