-- Sessões: versão de token (invalida JWTs antigos) e famílias de refresh para rotação/reuse detection
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0;

ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS family_id UUID;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens(family_id);

-- Tokens legados: cada linha é a sua própria família
UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL;
