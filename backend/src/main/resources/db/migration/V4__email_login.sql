ALTER TABLE account_member
  ADD COLUMN email TEXT;

CREATE UNIQUE INDEX ux_account_member_email_lower
  ON account_member (LOWER(email))
  WHERE email IS NOT NULL;