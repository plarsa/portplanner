ALTER TABLE boats RENAME COLUMN name TO model;
ALTER TABLE boats DROP COLUMN IF EXISTS registration_number;
