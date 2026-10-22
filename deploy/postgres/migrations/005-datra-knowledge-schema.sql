-- Execute no banco home_davi. A aplicação lê somente documentos APPROVED por MCP.
BEGIN;

CREATE SCHEMA IF NOT EXISTS datra;

CREATE TABLE IF NOT EXISTS datra.documents (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    content TEXT NOT NULL CHECK (length(trim(content)) > 0),
    source_url TEXT,
    source_reference TEXT,
    tags TEXT,
    status TEXT NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'ARCHIVED')),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT documents_approval_timestamp_check CHECK (
        (status = 'APPROVED' AND approved_at IS NOT NULL)
        OR (status <> 'APPROVED' AND approved_at IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS documents_approved_recent_idx
    ON datra.documents (approved_at DESC, updated_at DESC)
    WHERE status = 'APPROVED';

GRANT USAGE ON SCHEMA datra TO home_davi;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA datra TO home_davi;

COMMIT;
