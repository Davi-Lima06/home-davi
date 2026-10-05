package br.com.davi.homedavi.finance.domain;

import java.util.UUID;

/**
 * Normalized event accepted from Pluggy; the raw JSON is retained for audit and later enrichment.
 */
public record PluggyWebhookEvent(
    UUID eventId,
    String eventType,
    UUID itemId,
    UUID accountId,
    UUID clientId,
    String clientUserId,
    String triggeredBy,
    String transactionIdsJson,
    String rawPayloadJson) {}
