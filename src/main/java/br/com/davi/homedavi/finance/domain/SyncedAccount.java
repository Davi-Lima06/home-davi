package br.com.davi.homedavi.finance.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Conta como informada pelo provedor financeiro (Pluggy); o JSON bruto é mantido para auditoria.
 */
public record SyncedAccount(
    UUID id,
    String name,
    String type,
    String subtype,
    String numberMasked,
    String currencyCode,
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal creditLimit,
    String status,
    String rawDataJson) {
  public SyncedAccount {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    currencyCode = currencyCode == null || currencyCode.isBlank() ? "BRL" : currencyCode;
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
