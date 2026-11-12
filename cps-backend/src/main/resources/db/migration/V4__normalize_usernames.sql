-- Preserve IDs and relations. Case-folding collisions deliberately abort migration:
-- resolve them explicitly rather than renaming accounts or merging their orders.
UPDATE users SET username = LOWER(TRIM(username));
ALTER TABLE users ADD CONSTRAINT ck_users_canonical_username
    CHECK (username = LOWER(TRIM(username)));
-- Legacy users with unsupported names remain disabled until renamed by an administrator.
ALTER TABLE users ADD CONSTRAINT ck_users_enabled_username
    CHECK (NOT enabled OR REGEXP_LIKE(username, '^[a-z0-9._-]{1,64}$'));
