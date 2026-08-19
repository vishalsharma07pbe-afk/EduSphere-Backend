CREATE TABLE role_removal_requests (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    requested_role VARCHAR(50) NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_removal_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_role_removal_requests_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX uk_role_removal_pending_request
    ON role_removal_requests (
        organization_id,
        user_id,
        requested_role
    )
    WHERE status = 'PENDING';

CREATE INDEX idx_role_removal_requests_org_status_role
    ON role_removal_requests (
        organization_id,
        status,
        requested_role,
        created_at
    );

CREATE INDEX idx_role_removal_requests_requester
    ON role_removal_requests (
        organization_id,
        requested_by_user_id,
        created_at
    );

CREATE INDEX idx_role_removal_requests_target
    ON role_removal_requests (
        organization_id,
        user_id,
        created_at
    );

CREATE TABLE role_removal_approvals (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    approver_role VARCHAR(50) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    remarks VARCHAR(500),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_removal_approvals_request
        FOREIGN KEY (request_id) REFERENCES role_removal_requests(id),
    CONSTRAINT fk_role_removal_approvals_approver
        FOREIGN KEY (approver_user_id) REFERENCES users(id),
    CONSTRAINT uk_role_removal_approval_request_approver
        UNIQUE (request_id, approver_user_id)
);

CREATE INDEX idx_role_removal_approvals_request
    ON role_removal_approvals (request_id, decided_at);

CREATE INDEX idx_role_removal_approvals_approver
    ON role_removal_approvals (approver_user_id, decided_at);
