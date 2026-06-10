-- Configurações de monetização editáveis pelo admin (single-row)
CREATE TABLE monetization_settings (
    id              VARCHAR(32) PRIMARY KEY DEFAULT 'default',
    company_name    VARCHAR(200) NOT NULL DEFAULT 'Kumbu Lda',
    reference_prefix VARCHAR(8) NOT NULL DEFAULT 'KMB',
    gate_min_dau    INT NOT NULL DEFAULT 200,
    gate_min_listings INT NOT NULL DEFAULT 300,
    gate_min_chats  INT NOT NULL DEFAULT 30,
    default_max_listings INT NOT NULL DEFAULT 3,
    payment_expiry_hours INT NOT NULL DEFAULT 48,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      UUID
);

INSERT INTO monetization_settings (id) VALUES ('default');
