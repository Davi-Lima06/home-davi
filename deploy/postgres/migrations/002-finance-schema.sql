-- Execute no banco home_davi. Este script cria somente o schema financeiro.
BEGIN;

CREATE SCHEMA IF NOT EXISTS finance;

CREATE TABLE finance.accounts (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    name TEXT NOT NULL,
    type TEXT,
    subtype TEXT,
    number_masked TEXT,
    currency_code CHAR(3) NOT NULL DEFAULT 'BRL',
    current_balance NUMERIC(19, 4),
    available_balance NUMERIC(19, 4),
    credit_limit NUMERIC(19, 4),
    status TEXT,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX accounts_item_idx ON finance.accounts (item_id);

CREATE TABLE finance.categories (
    -- ID gerado pela aplicação; não exige a extensão pgcrypto no banco.
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    color TEXT,
    icon TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (name, kind)
);

CREATE TABLE finance.transactions (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    account_id UUID NOT NULL REFERENCES finance.accounts(id),
    category_id UUID REFERENCES finance.categories(id),
    description TEXT NOT NULL,
    merchant_name TEXT,
    amount NUMERIC(19, 4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'BRL',
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'UNKNOWN')),
    status TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    posted_at TIMESTAMPTZ,
    is_pending BOOLEAN NOT NULL DEFAULT false,
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX transactions_account_occurred_idx ON finance.transactions (account_id, occurred_at DESC);
CREATE INDEX transactions_item_occurred_idx ON finance.transactions (item_id, occurred_at DESC);
CREATE INDEX transactions_category_occurred_idx ON finance.transactions (category_id, occurred_at DESC);
CREATE INDEX transactions_pending_idx ON finance.transactions (is_pending) WHERE is_pending = true;

CREATE TABLE finance.transaction_sync_log (
    id BIGSERIAL PRIMARY KEY,
    webhook_event_id UUID REFERENCES integration.webhook_inbox(event_id),
    item_id UUID NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    error_message TEXT
);

GRANT USAGE ON SCHEMA finance TO home_davi;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA finance TO home_davi;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA finance TO home_davi;

COMMIT;
