-- Refactor BrokerHub to support multiple accounts per user.
-- Introduces "users" table and links "account_member" to it.
-- Note: email is stored and validated in lowercase by backend.

-- 1) Create new "users" table for global identity
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    login_id text UNIQUE NOT NULL,
    email text UNIQUE,
    password_hash text NOT NULL,
    member_name text NOT NULL
);

-- 2) Add user_id column to account_member
ALTER TABLE account_member
    ADD COLUMN user_id uuid;

-- 3) Backfill users from existing account_member data
INSERT INTO users (login_id, email, password_hash, member_name)
SELECT login_id, email, password_hash, member_name
FROM account_member;

-- 4) Link account_member to users
UPDATE account_member am
SET user_id = u.id
FROM users u
WHERE am.login_id = u.login_id;

-- 5) Add foreign key constraint
ALTER TABLE account_member
    ADD CONSTRAINT fk_account_member_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

-- 6) Index for faster lookups
CREATE INDEX IF NOT EXISTS idx_account_member_user_id ON account_member(user_id);