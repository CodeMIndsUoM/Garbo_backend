Database migration: add `zone` column to `admins_new`

Run these steps against your PostgreSQL database before deploying the code changes.

1) Add the `zone` column (VARCHAR(10)) to `admins_new`:

```sql
ALTER TABLE admins_new
  ADD COLUMN zone VARCHAR(10);
```

2) Backfill values from `council` to `zone` using a safe mapping:

```sql
UPDATE admins_new
SET zone = CASE
  WHEN council ILIKE 'Colombo' THEN 'Zone A'
  WHEN council ILIKE 'Galle' THEN 'Zone B'
  WHEN council ILIKE 'Kandy' THEN 'Zone C'
  ELSE NULL
END
WHERE zone IS NULL;
```

3) Verify results before committing to production:

```sql
SELECT council, zone, count(*) FROM admins_new GROUP BY council, zone ORDER BY council;
```

Notes:
- Do NOT drop or rename the existing `council` column yet; keep it for backward compatibility.
- From now on, the application will write/read the `zone` field for new admin creation; older parts of the system can continue reading `council` until fully migrated.

If you use Flyway or Liquibase, convert these statements into the appropriate migration file (e.g., `V2__add_zone_to_admins_new.sql`).
