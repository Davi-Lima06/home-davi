package br.com.davi.homedavi.datra.domain.messages;

import java.time.Instant;

/** A message received from the Datra Supabase table, identified by the source table primary key. */
public record DatraMessage(
    long sourceMessageId,
    String phone,
    MessageDirection direction,
    String content,
    Instant occurredAt,
    String sourceEventType,
    String rawPayload,
    Instant receivedAt,
    Instant updatedAt) {}
