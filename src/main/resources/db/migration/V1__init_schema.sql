-- Kumbu Backend — Schema inicial (adaptado de kumbu-admin/supabase/full_schema.sql)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------------
-- Utilizadores
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT UNIQUE,
    password_hash       TEXT,
    display_name        TEXT,
    phone               TEXT,
    photo_url           TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cart                JSONB NOT NULL DEFAULT '[]'::JSONB,
    favorites           JSONB NOT NULL DEFAULT '[]'::JSONB,
    delivery_address    JSONB,
    deleted_at          TIMESTAMPTZ,
    gender              TEXT,
    birth_date          DATE,
    city                TEXT,
    region              TEXT,
    country             TEXT,
    signup_source       TEXT NOT NULL DEFAULT 'unknown'
        CHECK (signup_source IN ('app', 'web', 'unknown')),
    signup_auth_method  TEXT NOT NULL DEFAULT 'email'
        CHECK (signup_auth_method IN ('email', 'google', 'facebook', 'phone', 'apple', 'anonymous', 'unknown')),
    last_active_source  TEXT NOT NULL DEFAULT 'unknown'
        CHECK (last_active_source IN ('app', 'web', 'unknown')),
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    banned_at           TIMESTAMPTZ,
    banned_until        TIMESTAMPTZ,
    ban_reason          TEXT,
    banned_by           UUID REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_created ON users (created_at DESC);
CREATE INDEX idx_users_banned_at ON users (banned_at) WHERE banned_at IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------------
CREATE TABLE catalog_categories (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    icon_key    TEXT NOT NULL DEFAULT 'category',
    accent_hex  TEXT NOT NULL DEFAULT 'C62828',
    sort_order  INT NOT NULL DEFAULT 0,
    kind        TEXT NOT NULL DEFAULT 'product'
        CHECK (kind IN ('product', 'stay', 'job'))
);

CREATE TABLE catalog_subcategories (
    category_id TEXT NOT NULL REFERENCES catalog_categories (id) ON DELETE CASCADE,
    id          TEXT NOT NULL,
    label       TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id, id)
);

CREATE TABLE catalog_products (
    id                  TEXT PRIMARY KEY,
    category_id         TEXT NOT NULL REFERENCES catalog_categories (id) ON DELETE RESTRICT,
    subcategory_id      TEXT,
    title               TEXT NOT NULL,
    rating              NUMERIC(3, 2) NOT NULL DEFAULT 4.5,
    price_label         TEXT NOT NULL,
    old_price_label     TEXT,
    discount_percent    INT,
    delivery_text       TEXT,
    image_color         BIGINT,
    image_url           TEXT,
    image_urls          JSONB NOT NULL DEFAULT '[]'::JSONB,
    description         TEXT,
    is_featured         BOOLEAN NOT NULL DEFAULT FALSE,
    is_out_of_stock     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order          INT NOT NULL DEFAULT 0,
    seller_id           UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    listing_kind        TEXT NOT NULL DEFAULT 'general'
        CHECK (listing_kind IN ('general', 'property', 'job')),
    property_meta       JSONB,
    job_meta            JSONB,
    job_listing_status  TEXT DEFAULT 'active'
        CHECK (job_listing_status IN ('active', 'filled_hidden')),
    product_meta        JSONB,
    view_count          INT NOT NULL DEFAULT 0,
    review_count        INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_catalog_products_category ON catalog_products (category_id);
CREATE INDEX idx_catalog_products_seller ON catalog_products (seller_id);
CREATE INDEX idx_catalog_products_listing_kind ON catalog_products (listing_kind) WHERE deleted_at IS NULL;
CREATE INDEX idx_catalog_products_featured ON catalog_products (is_featured) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Encomendas
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    id           TEXT PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seller_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    items_count  INT NOT NULL DEFAULT 1,
    total_label  TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'processing'
        CHECK (status IN ('processing', 'shipping', 'delivered', 'cancelled')),
    show_track   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_seller ON orders (seller_id);
CREATE INDEX idx_orders_created ON orders (created_at DESC);

CREATE TABLE order_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    TEXT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id  TEXT NOT NULL REFERENCES catalog_products (id) ON DELETE RESTRICT,
    quantity    INT NOT NULL DEFAULT 1,
    price_label TEXT NOT NULL,
    title       TEXT NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- ---------------------------------------------------------------------------
-- Notificações
-- ---------------------------------------------------------------------------
CREATE TABLE user_notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    body         TEXT NOT NULL,
    icon_key     TEXT NOT NULL DEFAULT 'notifications_outlined',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at      TIMESTAMPTZ,
    hidden_at    TIMESTAMPTZ,
    broadcast_id UUID,
    action_url   TEXT
);

CREATE INDEX idx_user_notifications_user ON user_notifications (user_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Configuração da app
-- ---------------------------------------------------------------------------
CREATE TABLE app_marketing_blocks (
    id            TEXT PRIMARY KEY,
    kind          TEXT NOT NULL CHECK (kind IN ('hero', 'offers')),
    title         TEXT NOT NULL,
    subtitle      TEXT NOT NULL DEFAULT '',
    gradient_from TEXT NOT NULL,
    gradient_to   TEXT NOT NULL,
    sort_order    INT NOT NULL DEFAULT 0
);

CREATE TABLE app_support_settings (
    id                 TEXT PRIMARY KEY DEFAULT 'default',
    welcome_message    TEXT NOT NULL DEFAULT 'Olá! Como podemos ajudar?',
    quick_actions      JSONB NOT NULL DEFAULT '[]'::JSONB,
    auto_reply_message TEXT NOT NULL DEFAULT ''
);

INSERT INTO app_support_settings (id) VALUES ('default') ON CONFLICT DO NOTHING;

CREATE TABLE app_payment_methods (
    id          TEXT PRIMARY KEY,
    label       TEXT NOT NULL,
    icon_key    TEXT NOT NULL DEFAULT 'payment',
    sort_order  INT NOT NULL DEFAULT 0,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE app_category_sort_filters (
    id         TEXT PRIMARY KEY,
    label      TEXT NOT NULL,
    sort_mode  TEXT NOT NULL DEFAULT 'default'
        CHECK (sort_mode IN ('default', 'rating_desc', 'price_asc')),
    sort_order INT NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- Admin
-- ---------------------------------------------------------------------------
CREATE TABLE admin_users (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    email      TEXT NOT NULL,
    role       TEXT NOT NULL DEFAULT 'admin'
        CHECK (role IN ('super_admin', 'admin', 'support')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_users_email ON admin_users (email);

CREATE TABLE admin_audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID REFERENCES users (id) ON DELETE SET NULL,
    actor_email TEXT,
    action      TEXT NOT NULL,
    entity      TEXT,
    entity_id   TEXT,
    payload     JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_log_created ON admin_audit_log (created_at DESC);

-- ---------------------------------------------------------------------------
-- Chat
-- ---------------------------------------------------------------------------
CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      TEXT REFERENCES catalog_products (id) ON DELETE SET NULL,
    buyer_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seller_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_blocked      BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason  TEXT,
    blocked_at      TIMESTAMPTZ,
    blocked_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    deal_status     TEXT NOT NULL DEFAULT 'open'
        CHECK (deal_status IN ('open', 'purchased', 'rejected')),
    deal_status_at  TIMESTAMPTZ,
    deal_status_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT conversations_buyer_seller CHECK (buyer_id <> seller_id),
    CONSTRAINT conversations_unique_product_buyer UNIQUE (product_id, buyer_id)
);

CREATE INDEX idx_conversations_buyer ON conversations (buyer_id);
CREATE INDEX idx_conversations_seller ON conversations (seller_id);
CREATE INDEX idx_conversations_updated ON conversations (updated_at DESC);

CREATE TABLE chat_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body             TEXT NOT NULL CHECK (char_length(trim(body)) > 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at          TIMESTAMPTZ,
    hidden_at        TIMESTAMPTZ,
    hidden_by        UUID REFERENCES users (id) ON DELETE SET NULL,
    message_kind     TEXT NOT NULL DEFAULT 'text'
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages (conversation_id, created_at);
CREATE INDEX idx_chat_messages_sender ON chat_messages (sender_id);

-- ---------------------------------------------------------------------------
-- Imóveis / Empregos / Reviews
-- ---------------------------------------------------------------------------
CREATE TABLE property_rental_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id       TEXT NOT NULL REFERENCES catalog_products (id) ON DELETE CASCADE,
    renter_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    owner_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rental_mode      TEXT NOT NULL CHECK (rental_mode IN ('daily', 'long_term')),
    check_in         DATE,
    check_out        DATE,
    nights           INT,
    guest_message    TEXT,
    status           TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'confirmed', 'rejected', 'cancelled')),
    conversation_id  UUID REFERENCES conversations (id) ON DELETE SET NULL,
    price_snapshot   TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_property_rental_product_dates
    ON property_rental_requests (product_id, check_in, check_out)
    WHERE status IN ('pending', 'confirmed');

CREATE TABLE user_cvs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    full_name   TEXT NOT NULL,
    profession  TEXT,
    email       TEXT,
    phone       TEXT,
    city        TEXT,
    province    TEXT,
    summary     TEXT,
    skills      JSONB NOT NULL DEFAULT '[]'::JSONB,
    languages   JSONB NOT NULL DEFAULT '[]'::JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_cvs_user ON user_cvs (user_id);

CREATE TABLE job_applications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id           TEXT NOT NULL REFERENCES catalog_products (id) ON DELETE CASCADE,
    applicant_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    employer_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cv_id            UUID REFERENCES user_cvs (id) ON DELETE SET NULL,
    status           TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected')),
    cover_message    TEXT,
    conversation_id  UUID REFERENCES conversations (id) ON DELETE SET NULL,
    cv_snapshot      JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_applications_applicant ON job_applications (applicant_id);
CREATE INDEX idx_job_applications_employer ON job_applications (employer_id);

CREATE TABLE product_reviews (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id       TEXT NOT NULL REFERENCES catalog_products (id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating           INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          TEXT,
    media            JSONB NOT NULL DEFAULT '[]'::JSONB,
    seller_reply     TEXT,
    seller_reply_at  TIMESTAMPTZ,
    conversation_id  UUID REFERENCES conversations (id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT product_reviews_unique_user_product UNIQUE (product_id, user_id)
);

CREATE INDEX idx_product_reviews_product ON product_reviews (product_id);

-- ---------------------------------------------------------------------------
-- Compliance
-- ---------------------------------------------------------------------------
CREATE TABLE content_reports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type      TEXT NOT NULL
        CHECK (target_type IN ('listing', 'user', 'conversation', 'message')),
    target_id        TEXT NOT NULL,
    reported_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    reason           TEXT NOT NULL
        CHECK (reason IN ('spam', 'fraud', 'illegal', 'harassment', 'misleading', 'ip', 'other')),
    details          TEXT,
    status           TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'reviewing', 'resolved', 'dismissed')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at      TIMESTAMPTZ,
    reviewed_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    admin_notes      TEXT
);

CREATE TABLE user_consents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    consent_type  TEXT NOT NULL,
    accepted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_agent    TEXT
);

CREATE TABLE user_blocks (
    blocker_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (blocker_id, blocked_id)
);

CREATE TABLE legal_documents (
    slug       TEXT PRIMARY KEY,
    title      TEXT NOT NULL,
    intro      TEXT,
    sections   JSONB NOT NULL DEFAULT '[]'::JSONB,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE user_deletion_events (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID NOT NULL,
    email      TEXT,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source     TEXT NOT NULL DEFAULT 'unknown'
        CHECK (source IN ('app', 'admin', 'unknown'))
);

CREATE INDEX idx_user_deletion_events_deleted ON user_deletion_events (deleted_at DESC);

-- ---------------------------------------------------------------------------
-- Refresh tokens
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
