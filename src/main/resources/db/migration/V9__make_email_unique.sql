-- Step 1: Clean up existing duplicate emails by appending '+dup<emp_id>'
WITH RankedUsers AS (
    SELECT emp_id, email,
           ROW_NUMBER() OVER(PARTITION BY LOWER(email) ORDER BY emp_id ASC) as rn
    FROM users
    WHERE email IS NOT NULL AND email <> ''
)
UPDATE users u
SET email = regexp_replace(ru.email, '@', '+dup' || ru.emp_id || '@')
FROM RankedUsers ru
WHERE u.emp_id = ru.emp_id AND ru.rn > 1;

-- Step 2: Create a unique index on LOWER(email) to enforce case-insensitive uniqueness
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
