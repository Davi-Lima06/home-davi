package br.com.davi.homedavi.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transação como informada pelo provedor financeiro; amount é sempre positivo e type indica a
 * direção.
 */
public record SyncedTransaction(
    UUID id,
    UUID accountId,
    String description,
    String merchantName,
    String categoryName,
    BigDecimal amount,
    String currencyCode,
    TransactionType type,
    String status,
    Instant occurredAt,
    boolean pending,
    String rawDataJson) {
  public SyncedTransaction {
    Objects.requireNonNull(id);
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(description);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(type);
    Objects.requireNonNull(occurredAt);
    amount = amount.abs();
    currencyCode = currencyCode == null || currencyCode.isBlank() ? "BRL" : currencyCode;
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
