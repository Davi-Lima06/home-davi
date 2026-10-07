package br.com.davi.homedavi.finance.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Investimento informado pelo Pluggy; o JSON bruto preserva os campos específicos do tipo. */
public record SyncedInvestment(
    UUID id, String name, String type, BigDecimal balance, String currencyCode, String rawDataJson) {
  public SyncedInvestment {
    Objects.requireNonNull(id);
    currencyCode = currencyCode == null || currencyCode.isBlank() ? "BRL" : currencyCode;
    rawDataJson = rawDataJson == null ? "{}" : rawDataJson;
  }
}
