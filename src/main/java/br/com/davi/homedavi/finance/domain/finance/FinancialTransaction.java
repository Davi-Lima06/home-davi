package br.com.davi.homedavi.finance.domain.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FinancialTransaction(
    UUID id,
    String accountId,
    String description,
    BigDecimal amount,
    String currency,
    TransactionType type,
    Instant occurredAt,
    String externalId) {
  public FinancialTransaction {
    Objects.requireNonNull(id);
    Objects.requireNonNull(accountId);
    Objects.requireNonNull(description);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(type);
    Objects.requireNonNull(occurredAt);
    if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    currency = currency == null || currency.isBlank() ? "BRL" : currency;
  }
}
