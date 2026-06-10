-- Recomendações: histórico de visualizações + índices de localização

CREATE TABLE product_view_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id),
    product_id  VARCHAR(128) NOT NULL REFERENCES catalog_products(id),
    city        TEXT,
    region      TEXT,
    category_id VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_view_events_user ON product_view_events(user_id, created_at DESC);
CREATE INDEX idx_product_view_events_product ON product_view_events(product_id, created_at DESC);
CREATE INDEX idx_users_city ON users(LOWER(city)) WHERE city IS NOT NULL;
CREATE INDEX idx_users_region ON users(LOWER(region)) WHERE region IS NOT NULL;
CREATE INDEX idx_catalog_products_category_active ON catalog_products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_catalog_products_trending ON catalog_products(view_count DESC, created_at DESC) WHERE deleted_at IS NULL;
