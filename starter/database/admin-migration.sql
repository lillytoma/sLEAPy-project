-- Apply the admin role to databases created before the admin feature.
ALTER TABLE Clients
    ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Keep the development administrator available after upgrading an existing volume.
UPDATE Clients
SET is_admin = TRUE
WHERE email = 'jdoe@email.com'
  AND NOT EXISTS (SELECT 1 FROM Clients WHERE is_admin = TRUE);
