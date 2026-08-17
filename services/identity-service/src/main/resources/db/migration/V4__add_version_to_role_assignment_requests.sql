ALTER TABLE role_assignment_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE role_assignment_requests
    ALTER COLUMN version DROP DEFAULT;