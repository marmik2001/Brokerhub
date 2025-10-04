-- Removes redundant credential columns from account_member
-- and finalizes user_id linkage as mandatory.

-- 1) Ensure all rows are linked before making user_id NOT NULL
DO $$
DECLARE
    missing_count INT;
BEGIN
    SELECT COUNT(*) INTO missing_count FROM account_member WHERE user_id IS NULL;
    IF missing_count > 0 THEN
        RAISE EXCEPTION 'Cannot enforce NOT NULL: % account_member rows missing user_id', missing_count;
    END IF;
END $$;

-- 2) Make user_id mandatory now that backfill is complete
ALTER TABLE account_member
    ALTER COLUMN user_id SET NOT NULL;

-- 3) Drop redundant user credential columns
ALTER TABLE account_member
    DROP COLUMN IF EXISTS login_id,
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS member_name,
    DROP COLUMN IF EXISTS email;

-- 4) Drop old indexes that depended on these columns (if they exist)
DROP INDEX IF EXISTS idx_account_member_account_id;  -- optional reindex below
CREATE INDEX IF NOT EXISTS idx_account_member_account_id ON account_member(account_id);