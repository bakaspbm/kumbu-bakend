-- Tokens de autenticação (verificação email, reset password, OTP telefone)
CREATE TABLE auth_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users (id) ON DELETE CASCADE,
    email       TEXT,
    phone       TEXT,
    token_hash  TEXT NOT NULL,
    token_type  TEXT NOT NULL
        CHECK (token_type IN ('email_verify', 'password_reset', 'phone_otp')),
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_tokens_hash ON auth_tokens (token_hash);
CREATE INDEX idx_auth_tokens_user_type ON auth_tokens (user_id, token_type);

-- Tentativas de login (protecção brute-force)
CREATE TABLE login_attempts (
    id           BIGSERIAL PRIMARY KEY,
    identifier   TEXT NOT NULL,
    ip_address   TEXT,
    success      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_login_attempts_identifier ON login_attempts (identifier, created_at DESC);
