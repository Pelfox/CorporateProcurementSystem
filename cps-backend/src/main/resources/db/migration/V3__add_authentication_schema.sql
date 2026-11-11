-- Existing users have no credentials: preserve IDs/relations and disable login.
-- A password reset must provision an individual BCrypt hash before enabling them.
-- Keep password_hash nullable during this transition; never invent a shared password.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN temporary_password_expires_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD CONSTRAINT ck_users_enabled_credentials
    CHECK (NOT enabled OR password_hash IS NOT NULL);
ALTER TABLE users ADD CONSTRAINT ck_users_temporary_password_expiry
    CHECK (NOT enabled OR NOT must_change_password OR temporary_password_expires_at IS NOT NULL);

CREATE TABLE auth_sessions (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    session_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_auth_sessions PRIMARY KEY (id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_auth_sessions_type CHECK (session_type IN ('FULL', 'PASSWORD_CHANGE'))
);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    session_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES auth_sessions (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_hash_length CHECK (CHAR_LENGTH(token_hash) = 64)
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens (session_id);
