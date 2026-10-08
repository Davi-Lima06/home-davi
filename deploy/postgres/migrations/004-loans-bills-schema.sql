-- Execute no banco home_davi. Cria as tabelas de empréstimos e faturas no schema finance,
-- populadas quando uma conta nova é detectada ao receber um webhook da Pluggy.
BEGIN;

CREATE TABLE finance.loans (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    contract_number TEXT,
    outstanding_balance NUMERIC(19, 4),
    currency_code CHAR(3) NOT NULL DEFAULT 'BRL',
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX loans_item_idx ON finance.loans (item_id);

CREATE TABLE finance.bills (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES finance.accounts(id),
    due_date DATE,
    total_amount NUMERIC(19, 4),
    minimum_payment NUMERIC(19, 4),
    currency_code CHAR(3) NOT NULL DEFAULT 'BRL',
    raw_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX bills_account_idx ON finance.bills (account_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON finance.loans, finance.bills TO home_davi;

COMMIT;
