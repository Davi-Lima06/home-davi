package br.com.davi.homedavi.datra.domain;

import java.time.Instant;
import java.util.UUID;

/** An auditable, human-approved knowledge source for the Datra profile. */
public record DatraDocument(
    UUID id,
    String title,
    String content,
    String sourceUrl,
    String sourceReference,
    String tags,
    DatraDocumentStatus status,
    Instant approvedAt,
    Instant createdAt,
    Instant updatedAt) {}
