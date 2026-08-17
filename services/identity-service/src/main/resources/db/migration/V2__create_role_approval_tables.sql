CREATE TABLE role_assignment_requests (
                                          id BIGSERIAL PRIMARY KEY,

                                          organization_id BIGINT NOT NULL,
                                          user_id BIGINT NOT NULL,
                                          requested_role VARCHAR(50) NOT NULL,
                                          requested_by_user_id BIGINT NOT NULL,
                                          status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

                                          completed_at TIMESTAMP WITH TIME ZONE,
                                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_role_request_user
                                              FOREIGN KEY (user_id)
                                                  REFERENCES users(id),

                                          CONSTRAINT fk_role_request_requested_by
                                              FOREIGN KEY (requested_by_user_id)
                                                  REFERENCES users(id),

                                          CONSTRAINT chk_role_request_status
                                              CHECK (
                                                  status IN (
                                                             'PENDING',
                                                             'APPROVED',
                                                             'REJECTED',
                                                             'CANCELLED'
                                                      )
                                                  )
);

CREATE TABLE role_assignment_approvals (
                                           id BIGSERIAL PRIMARY KEY,

                                           request_id BIGINT NOT NULL,
                                           approver_user_id BIGINT NOT NULL,
                                           approver_role VARCHAR(50) NOT NULL,
                                           decision VARCHAR(20) NOT NULL,
                                           remarks VARCHAR(500),
                                           decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_role_approval_request
                                               FOREIGN KEY (request_id)
                                                   REFERENCES role_assignment_requests(id),

                                           CONSTRAINT fk_role_approval_user
                                               FOREIGN KEY (approver_user_id)
                                                   REFERENCES users(id),

                                           CONSTRAINT uk_role_approval_request_approver
                                               UNIQUE (request_id, approver_user_id),

                                           CONSTRAINT chk_role_approval_decision
                                               CHECK (
                                                   decision IN (
                                                                'APPROVED',
                                                                'REJECTED'
                                                       )
                                                   )
);

CREATE INDEX idx_role_requests_organization_status
    ON role_assignment_requests (
                                 organization_id,
                                 status
        );

CREATE INDEX idx_role_requests_user
    ON role_assignment_requests (user_id);

CREATE INDEX idx_role_approvals_request
    ON role_assignment_approvals (request_id);