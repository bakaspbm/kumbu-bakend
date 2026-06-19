-- Suporte sem login: conversas de visitantes com token de acesso

ALTER TABLE conversations
    ALTER COLUMN buyer_id DROP NOT NULL;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS guest_name TEXT,
    ADD COLUMN IF NOT EXISTS guest_email TEXT,
    ADD COLUMN IF NOT EXISTS guest_access_token TEXT;

DROP INDEX IF EXISTS conversations_unique_support_user;

CREATE UNIQUE INDEX IF NOT EXISTS conversations_unique_support_user
    ON conversations (buyer_id)
    WHERE conversation_type = 'support' AND buyer_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS conversations_unique_support_guest_token
    ON conversations (guest_access_token)
    WHERE conversation_type = 'support' AND guest_access_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_conversations_support_guest_email
    ON conversations (guest_email)
    WHERE conversation_type = 'support' AND guest_email IS NOT NULL;

INSERT INTO users (
    id, email, display_name, signup_source, signup_auth_method, last_active_source,
    email_verified, phone_verified, cart, favorites, created_at, updated_at
)
VALUES (
    '00000000-0000-4000-8000-000000000003',
    'visitante@kumbu.internal',
    'Visitante',
    'unknown',
    'unknown',
    'unknown',
    FALSE,
    FALSE,
    '[]'::JSONB,
    '[]'::JSONB,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;
