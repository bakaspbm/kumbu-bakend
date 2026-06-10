-- Categorias em falta + estratégias de monetização por categoria

INSERT INTO catalog_categories (id, name, icon_key, accent_hex, sort_order, kind) VALUES
    ('eletrodomesticos', 'Eletrodomésticos', 'kitchen', '546E7A', 10, 'product'),
    ('beleza', 'Beleza', 'face', 'C2185B', 11, 'product')
ON CONFLICT DO NOTHING;

-- Estratégia por categoria (admin edita tudo aqui)
CREATE TABLE monetization_category_strategies (
    category_id             VARCHAR(64) PRIMARY KEY REFERENCES catalog_categories(id),
    primary_monetization    VARCHAR(64) NOT NULL,
    secondary_monetizations JSONB NOT NULL DEFAULT '[]',
    strategy_title          VARCHAR(200) NOT NULL,
    strategy_description    TEXT,
    why_description         TEXT,
    cta_message             VARCHAR(300),
    cta_button_label        VARCHAR(120),
    revenue_tier            VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    enabled_feature_types   JSONB NOT NULL DEFAULT '[]',
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order              INT NOT NULL DEFAULT 0,
    metadata                JSONB NOT NULL DEFAULT '{}',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by              UUID
);

-- Produtos passam a ser por categoria (preço diferente por categoria)
ALTER TABLE monetization_products ADD COLUMN IF NOT EXISTS category_id VARCHAR(64) REFERENCES catalog_categories(id);
CREATE INDEX IF NOT EXISTS idx_monetization_products_category ON monetization_products(category_id);
