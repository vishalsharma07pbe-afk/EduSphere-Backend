CREATE TABLE role_permissions (
  id BIGSERIAL PRIMARY KEY,
  role VARCHAR(100) NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE
      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_role_permissions_role_permission
      UNIQUE (role, permission_id),
  CONSTRAINT fk_role_permissions_permission
      FOREIGN KEY (permission_id)
          REFERENCES permissions (id)
          ON DELETE RESTRICT
);