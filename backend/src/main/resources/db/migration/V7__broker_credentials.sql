-- Flyway migration: create account_member_brokers table for storing envelope-encrypted broker credentials.

CREATE TABLE IF NOT EXISTS account_member_brokers (
    credential_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_member_id UUID NOT NULL REFERENCES account_member(id) ON DELETE CASCADE,
    broker VARCHAR(50) NOT NULL,
    nickname VARCHAR(256) NOT NULL,
    token_cipher BYTEA NOT NULL,
    token_iv BYTEA NOT NULL,
    token_encrypted_dek BYTEA NOT NULL,
    token_key_id VARCHAR(256) NOT NULL
);

-- index for fast lookups by account member
CREATE INDEX IF NOT EXISTS idx_account_member_brokers_account_member_id
    ON account_member_brokers (account_member_id);
