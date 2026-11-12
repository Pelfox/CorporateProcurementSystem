ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (user_role IN ('USER', 'MANAGER', 'ADMIN'));

-- Serializes administrator membership changes across backend instances.
CREATE TABLE security_admin_guard (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);
INSERT INTO security_admin_guard (id) VALUES (1);
