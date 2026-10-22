-- Recepção idempotente do Database Webhook do Supabase para public.logs_mensagens.
BEGIN;

CREATE TABLE IF NOT EXISTS datra.message_inbox (
    source_message_id BIGINT PRIMARY KEY,
    phone VARCHAR(15) NOT NULL CHECK (phone ~ '^[0-9]{10,15}$'),
    direction TEXT NOT NULL CHECK (direction IN ('ENTRADA', 'SAIDA')),
    content TEXT NOT NULL CHECK (length(trim(content)) BETWEEN 1 AND 10000),
    occurred_at TIMESTAMPTZ NOT NULL,
    source_event_type VARCHAR(10) NOT NULL CHECK (source_event_type IN ('INSERT', 'UPDATE')),
    raw_payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS message_inbox_occurred_at_idx
    ON datra.message_inbox (occurred_at DESC);

CREATE INDEX IF NOT EXISTS message_inbox_phone_idx
    ON datra.message_inbox (phone);

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA datra TO home_davi;

COMMIT;
