ALTER TABLE role_assignment_requests
    ADD COLUMN reason VARCHAR(500)
        NOT NULL
        DEFAULT 'Role assignment requested';

ALTER TABLE role_assignment_requests
    ALTER COLUMN reason DROP DEFAULT;