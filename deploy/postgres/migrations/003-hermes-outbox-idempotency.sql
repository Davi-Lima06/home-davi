-- Execute no banco home_davi depois de 001-webhook-schema.sql.
-- Garante no banco que cada evento Pluggy origine no máximo uma notificação para o Hermes.
CREATE UNIQUE INDEX IF NOT EXISTS hermes_outbox_webhook_event_uidx
    ON integration.hermes_outbox (webhook_event_id);
