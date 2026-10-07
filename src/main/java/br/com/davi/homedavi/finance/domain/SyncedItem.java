package br.com.davi.homedavi.finance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Conexão bancária (item) e seu estado, informada pelo Pluggy. */
public record SyncedItem(
    UUID id,
    String connectorName,
    String status,
    String executionStatus,
    Instant lastUpdatedAt,
    String rawDataJson) {
  public SyncedItem {
    Objects.requireNonNull(id);
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
