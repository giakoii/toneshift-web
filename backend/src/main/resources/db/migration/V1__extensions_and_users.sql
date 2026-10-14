CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email             citext        NOT NULL,
  name              varchar(100)  NOT NULL,
  password_hash     varchar(100),                       -- NULL với tài khoản chỉ Google
  image_url         varchar(500),
  role              varchar(10)   NOT NULL DEFAULT 'USER',
  status            varchar(10)   NOT NULL DEFAULT 'ACTIVE',
  email_verified_at timestamptz,
  created_at        timestamptz   NOT NULL DEFAULT now(),
  updated_at        timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_role   CHECK (role   IN ('USER','ADMIN')),
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','BLOCKED'))
);

CREATE TABLE auth_providers (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider             varchar(20)  NOT NULL,           -- GOOGLE
  provider_account_id  varchar(100) NOT NULL,           -- Google 'sub'
  created_at           timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT uq_provider_account UNIQUE (provider, provider_account_id),
  CONSTRAINT uq_provider_per_user UNIQUE (user_id, provider)
);

CREATE TABLE refresh_tokens (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash   char(64)    NOT NULL,                    -- SHA-256 hex
  family_id    uuid        NOT NULL,
  expires_at   timestamptz NOT NULL,
  revoked_at   timestamptz,
  replaced_by  uuid,
  user_agent   varchar(300),
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_user   ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_expiry ON refresh_tokens(expires_at);   -- job dọn rác

CREATE TABLE password_reset_tokens (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  char(64)    NOT NULL,
  expires_at  timestamptz NOT NULL,
  used_at     timestamptz,
  created_at  timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_reset_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_reset_user ON password_reset_tokens(user_id);
