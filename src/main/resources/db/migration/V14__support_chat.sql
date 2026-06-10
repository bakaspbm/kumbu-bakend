-- Suporte Kumbú: chat com FAQ automático + escalonamento para admin
-- Remove bloqueio entre utilizadores (apenas admin modera)

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS conversation_type TEXT NOT NULL DEFAULT 'marketplace'
        CHECK (conversation_type IN ('marketplace', 'support'));

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS support_status TEXT
        CHECK (support_status IS NULL OR support_status IN ('bot', 'waiting_admin', 'assigned', 'closed'));

ALTER TABLE conversations DROP CONSTRAINT IF EXISTS conversations_unique_product_buyer;

CREATE UNIQUE INDEX IF NOT EXISTS conversations_unique_marketplace_buyer
    ON conversations (product_id, buyer_id)
    WHERE conversation_type = 'marketplace' AND product_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS conversations_unique_support_user
    ON conversations (buyer_id)
    WHERE conversation_type = 'support';

CREATE INDEX IF NOT EXISTS idx_conversations_support_status
    ON conversations (support_status, updated_at DESC)
    WHERE conversation_type = 'support';

-- Conta sistema (mensagens automáticas / suporte visível ao cliente)
INSERT INTO users (
    id, email, display_name, signup_source, signup_auth_method, last_active_source,
    email_verified, phone_verified, cart, favorites, created_at, updated_at
)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'suporte@kumbu.internal',
    'Kumbú Suporte',
    'unknown',
    'unknown',
    'unknown',
    TRUE,
    FALSE,
    '[]'::JSONB,
    '[]'::JSONB,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- FAQ / perguntas rápidas na app
UPDATE app_support_settings
SET
    welcome_message = 'Olá! Sou o assistente Kumbú. Escolha um tema abaixo ou escreva a sua dúvida.',
    auto_reply_message = 'Recebemos a sua mensagem. Um agente de suporte irá responder em breve.',
    quick_actions = '[
      {
        "id": "how_buy",
        "label": "Como comprar?",
        "answer": "Navegue pelo catálogo, abra um anúncio e use «Contactar vendedor» ou «Comprar». O chat com o vendedor fica em Mensagens. Para imóveis e empregos há fluxos específicos no anúncio.",
        "keywords": ["comprar", "compra", "pedido", "encomenda"]
      },
      {
        "id": "how_sell",
        "label": "Como vender / publicar?",
        "answer": "Na conta, vá a «Vender» ou «Os meus anúncios», escolha a categoria e preencha título, fotos e preço. Algumas categorias podem exigir verificação ou taxa de publicação.",
        "keywords": ["vender", "anunciar", "publicar", "anúncio", "anuncio"]
      },
      {
        "id": "payments",
        "label": "Pagamentos e taxas",
        "answer": "O Kumbú pode aplicar taxas de contacto, destaque ou planos de vendedor conforme a categoria. Os pagamentos entre comprador e vendedor são acordados entre as partes, salvo indicação em contrário no anúncio.",
        "keywords": ["pagamento", "pagar", "taxa", "kwanza", "kz", "preço"]
      },
      {
        "id": "account",
        "label": "Conta, login e palavra-passe",
        "answer": "Pode entrar com email, Google ou Facebook. Para recuperar a palavra-passe use «Esqueceu?» no login. A mesma conta funciona na app e no site.",
        "keywords": ["conta", "login", "entrar", "palavra-passe", "senha", "email", "google", "facebook"]
      },
      {
        "id": "safety",
        "label": "Segurança e denúncias",
        "answer": "Use «Denunciar» num anúncio ou perfil suspeito. Não envie dinheiro fora da plataforma sem confiança. A equipa Kumbú analisa denúncias; apenas administradores podem suspender contas.",
        "keywords": ["denunciar", "fraude", "segurança", "bloquear", "suspeito"]
      },
      {
        "id": "human",
        "label": "Falar com suporte humano",
        "escalate": true,
        "answer": "Vou encaminhar a sua conversa para a nossa equipa. Aguarde — responderemos o mais breve possível."
      }
    ]'::JSONB
WHERE id = 'default';

DROP TABLE IF EXISTS user_blocks;
