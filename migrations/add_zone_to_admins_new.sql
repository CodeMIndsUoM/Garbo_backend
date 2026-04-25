-- Add `zone` column to admins_new and backfill from council
BEGIN;

ALTER TABLE admins_new
  ADD COLUMN IF NOT EXISTS zone VARCHAR(10);

UPDATE admins_new
SET zone = CASE
  WHEN council ILIKE 'Colombo' THEN 'Zone A'
  WHEN council ILIKE 'Galle' THEN 'Zone B'
  WHEN council ILIKE 'Kandy' THEN 'Zone C'
  ELSE NULL
END
WHERE zone IS NULL;

COMMIT;
