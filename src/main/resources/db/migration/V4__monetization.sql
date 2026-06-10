-- Monetização por fases — marketplace de leads (Angola, Kz)

-- Fases do roadmap
CREATE TABLE monetization_phases (
    id              VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    min_users       INT NOT NULL DEFAULT 0,
    max_users       INT,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Features monetizáveis (activáveis por fase)
CREATE TABLE monetization_features (
    id              VARCHAR(64) PRIMARY KEY,
    phase_id        VARCHAR(32) NOT NULL REFERENCES monetization_phases(id),
    feature_type    VARCHAR(64) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    activated_at    TIMESTAMPTZ,
    activated_by    UUID,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (phase_id, feature_type)
);

-- Catálogo de produtos/preços (Kz)
CREATE TABLE monetization_products (
    id              VARCHAR(64) PRIMARY KEY,
    feature_type    VARCHAR(64) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    description     TEXT,
    price_kz        BIGINT NOT NULL,
    duration_days   INT,
    max_listings    INT,
    category_hint   VARCHAR(64),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Provedores de pagamento locais (Multicaixa, bancos angolanos)
CREATE TABLE monetization_payment_providers (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    provider_type   VARCHAR(32) NOT NULL,
    bank_name       VARCHAR(120),
    account_holder  VARCHAR(200),
    account_number  VARCHAR(64),
    iban            VARCHAR(64),
    phone_number    VARCHAR(32),
    instructions    TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Pagamentos da plataforma (receita Kumbu)
CREATE TABLE monetization_payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    product_id      VARCHAR(64) NOT NULL REFERENCES monetization_products(id),
    provider_id     VARCHAR(64) REFERENCES monetization_payment_providers(id),
    amount_kz       BIGINT NOT NULL,
    reference_code  VARCHAR(32) NOT NULL UNIQUE,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(32),
    target_type     VARCHAR(32),
    target_id       VARCHAR(128),
    proof_url       TEXT,
    proof_note      TEXT,
    confirmed_at    TIMESTAMPTZ,
    confirmed_by    UUID,
    rejected_reason TEXT,
    expires_at      TIMESTAMPTZ NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monetization_payments_user ON monetization_payments(user_id);
CREATE INDEX idx_monetization_payments_status ON monetization_payments(status);
CREATE INDEX idx_monetization_payments_reference ON monetization_payments(reference_code);

-- Promoções aplicadas a anúncios (destaque, boost, urgente)
CREATE TABLE listing_promotions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      VARCHAR(128) NOT NULL REFERENCES catalog_products(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    payment_id      UUID REFERENCES monetization_payments(id),
    promotion_type  VARCHAR(32) NOT NULL,
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ends_at         TIMESTAMPTZ,
    boost_score     INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_promotions_product ON listing_promotions(product_id);
CREATE INDEX idx_listing_promotions_active ON listing_promotions(is_active, ends_at);

-- Subscrições VIP / Business
CREATE TABLE user_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    product_id      VARCHAR(64) NOT NULL REFERENCES monetization_products(id),
    payment_id      UUID REFERENCES monetization_payments(id),
    plan_type       VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    max_listings    INT NOT NULL DEFAULT 5,
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ends_at         TIMESTAMPTZ NOT NULL,
    auto_renew      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_subscriptions_user ON user_subscriptions(user_id, status);

-- Verificação de vendedor
CREATE TABLE seller_verifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    payment_id      UUID REFERENCES monetization_payments(id),
    tier            VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    document_url    TEXT,
    admin_note      TEXT,
    reviewed_at     TIMESTAMPTZ,
    reviewed_by     UUID,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_verifications_user ON seller_verifications(user_id);

-- Desbloqueio de contacto (Fase 4)
CREATE TABLE contact_unlocks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id        UUID NOT NULL REFERENCES users(id),
    seller_id       UUID NOT NULL REFERENCES users(id),
    product_id      VARCHAR(128) REFERENCES catalog_products(id),
    payment_id      UUID REFERENCES monetization_payments(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (buyer_id, seller_id, product_id)
);

-- Pedidos de aprovação para activar fases/features
CREATE TABLE monetization_approval_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type    VARCHAR(32) NOT NULL,
    phase_id        VARCHAR(32) REFERENCES monetization_phases(id),
    feature_id      VARCHAR(64) REFERENCES monetization_features(id),
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    metrics_snapshot JSONB NOT NULL DEFAULT '{}',
    message         TEXT,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ,
    reviewed_by     UUID,
    review_note     TEXT
);

CREATE INDEX idx_monetization_approval_status ON monetization_approval_requests(status);

-- Métricas diárias para gatilhos de monetização
CREATE TABLE monetization_daily_metrics (
    metric_date     DATE PRIMARY KEY,
    dau             INT NOT NULL DEFAULT 0,
    total_users     INT NOT NULL DEFAULT 0,
    active_listings INT NOT NULL DEFAULT 0,
    chats_today     INT NOT NULL DEFAULT 0,
    computed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Campos extra em users para monetização
ALTER TABLE users ADD COLUMN IF NOT EXISTS seller_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS seller_verification_tier VARCHAR(32);
ALTER TABLE users ADD COLUMN IF NOT EXISTS vip_until TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS business_plan_id VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS max_listings INT NOT NULL DEFAULT 3;

-- Campos extra em catalog_products para ranking pago
ALTER TABLE catalog_products ADD COLUMN IF NOT EXISTS featured_until TIMESTAMPTZ;
ALTER TABLE catalog_products ADD COLUMN IF NOT EXISTS highlight_type VARCHAR(32);
ALTER TABLE catalog_products ADD COLUMN IF NOT EXISTS boosted_until TIMESTAMPTZ;
ALTER TABLE catalog_products ADD COLUMN IF NOT EXISTS boost_score INT NOT NULL DEFAULT 0;
