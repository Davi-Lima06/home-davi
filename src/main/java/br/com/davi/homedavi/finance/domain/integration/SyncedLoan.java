package br.com.davi.homedavi.finance.domain.integration;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Empréstimo informado pelo Pluggy; o JSON bruto preserva parcelas e encargos. */
public record SyncedLoan(
    UUID id,
    String contractNumber,
    BigDecimal outstandingBalance,
    String currencyCode,
    String rawDataJson) {
  public SyncedLoan {
    Objects.requireNonNull(id);
    currencyCode = currencyCode == null || currencyCode.isBlank() ? "BRL" : currencyCode;
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
