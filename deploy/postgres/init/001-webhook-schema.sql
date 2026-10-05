CREATE SCHEMA IF NOT EXISTS integration;

CREATE TABLE integration.webhook_inbox (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type TEXT NOT NULL,
    item_id UUID,
    account_id UUID,
    client_id UUID,
    client_user_id TEXT,
    triggered_by TEXT,
    transaction_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    raw_payload JSONB NOT NULL,
    status TEXT NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX webhook_inbox_pending_idx
    ON integration.webhook_inbox (status, received_at)
    WHERE status IN ('RECEIVED', 'FAILED');

CREATE INDEX webhook_inbox_item_idx ON integration.webhook_inbox (item_id, received_at DESC);

CREATE TABLE integration.hermes_outbox (
    id BIGSERIAL PRIMARY KEY,
    webhook_event_id UUID NOT NULL REFERENCES integration.webhook_inbox(event_id),
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ,
    last_error TEXT
);

CREATE INDEX hermes_outbox_pending_idx
    ON integration.hermes_outbox (status, available_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE UNIQUE INDEX hermes_outbox_webhook_event_uidx
    ON integration.hermes_outbox (webhook_event_id);

GRANT USAGE ON SCHEMA integration TO home_davi;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA integration TO home_davi;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA integration TO home_davi;
