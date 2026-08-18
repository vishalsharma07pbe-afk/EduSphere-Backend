ALTER TABLE users
    ADD COLUMN login_lock_level INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD CONSTRAINT chk_users_login_lock_level
        CHECK (login_lock_level >= 0);
