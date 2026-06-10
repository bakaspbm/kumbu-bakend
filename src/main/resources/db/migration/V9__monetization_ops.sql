-- Gate de cobrança, Multicaixa primeiro, SLA pagamentos, métricas VIP/leads

ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS charging_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS default_payment_provider_id VARCHAR(64) NOT NULL DEFAULT 'prov_multicaixa_express';
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS payment_sla_hours INT NOT NULL DEFAULT 24;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS bank_transfers_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS leads_min_servicos_chats INT NOT NULL DEFAULT 100;

ALTER TABLE monetization_payment_providers ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE monetization_payment_providers SET is_default = TRUE, sort_order = 0 WHERE id = 'prov_multicaixa_express';
UPDATE monetization_payment_providers SET sort_order = 1 WHERE id = 'prov_emis_reference';
UPDATE monetization_payment_providers SET sort_order = 10 WHERE provider_type = 'BANK_TRANSFER';

-- Contactos gerados por VIP (serviços) — medir antes de activar leads pagos
CREATE TABLE IF NOT EXISTS monetization_vip_contact_stats (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    category_id     VARCHAR(64) NOT NULL DEFAULT 'servicos',
    period_month    DATE NOT NULL,
    contacts_count  INT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, category_id, period_month)
);

CREATE INDEX IF NOT EXISTS idx_vip_contact_stats_user ON monetization_vip_contact_stats(user_id, period_month);

-- Actualizar mensagem VIP serviços
UPDATE monetization_category_strategies
SET cta_message = 'Receba mais clientes — assine VIP',
    cta_button_label = 'Assinar VIP',
    strategy_description = 'VIP primeiro. Leads pagos só quando houver volume de procura.'
WHERE category_id = 'servicos';
