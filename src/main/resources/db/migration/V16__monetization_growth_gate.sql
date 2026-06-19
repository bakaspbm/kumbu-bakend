-- Limites superiores do intervalo de crescimento (UI) e estado de alerta ao superadmin
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS gate_max_dau INT NOT NULL DEFAULT 300;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS gate_max_listings INT NOT NULL DEFAULT 500;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS gate_max_chats INT NOT NULL DEFAULT 50;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS gate_last_ready BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE monetization_settings ADD COLUMN IF NOT EXISTS gate_alert_sent_at TIMESTAMPTZ;
