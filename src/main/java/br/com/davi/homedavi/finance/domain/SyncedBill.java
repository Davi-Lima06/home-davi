package br.com.davi.homedavi.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Fatura de cartão informada pelo Pluggy; o JSON bruto é mantido para auditoria. */
public record SyncedBill(
    UUID id,
    UUID accountId,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal minimumPayment,
    String currencyCode,
    String rawDataJson) {
  public SyncedBill {
    Objects.requireNonNull(id);
    currencyCode = currencyCode == null || currencyCode.isBlank() ? "BRL" : currencyCode;
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
