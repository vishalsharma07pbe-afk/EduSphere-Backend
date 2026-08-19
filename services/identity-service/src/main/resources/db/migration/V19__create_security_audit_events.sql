CREATE TABLE security_audit_events (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    action VARCHAR(80) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id BIGINT,
    request_id VARCHAR(120),
    ip_address VARCHAR(80),
    details VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_security_audit_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX idx_security_audit_events_org_occurred
    ON security_audit_events (organization_id, occurred_at DESC);

CREATE INDEX idx_security_audit_events_org_action
    ON security_audit_events (organization_id, action, occurred_at DESC);

CREATE INDEX idx_security_audit_events_org_target
    ON security_audit_events (organization_id, target_type, target_id);
