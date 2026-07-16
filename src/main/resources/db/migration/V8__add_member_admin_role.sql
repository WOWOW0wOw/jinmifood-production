ALTER TABLE members ADD COLUMN admin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE members
SET admin = TRUE
WHERE lower(email) = 'jongseok8529@gmail.com';
