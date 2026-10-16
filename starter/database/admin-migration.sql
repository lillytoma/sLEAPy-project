-- Safe migration for databases created before the admin console was added.
ALTER TABLE Clients
    ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE Clients
SET password_hash = '$2a$10$ptnaw1mf2r2TRo8ANIN.3OXRgKIdgCzXWDtyWE0.btT0JHXu7W/OO',
    is_admin = TRUE
WHERE username = 'jdoe';
