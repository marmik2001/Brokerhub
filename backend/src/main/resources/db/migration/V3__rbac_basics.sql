CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE accounts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  description text
);

CREATE TABLE account_member (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  login_id text UNIQUE NOT NULL,
  account_id uuid NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  member_name text NOT NULL,
  password_hash text NOT NULL,
  role text NOT NULL CHECK (role IN ('ADMIN','MEMBER')),
  rules jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_account_member_account_id ON account_member(account_id);

