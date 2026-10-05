package br.com.davi.homedavi.finance.domain;

import java.util.Objects;
import java.util.UUID;

/** Referência segura de uma conta já sincronizada no banco financeiro. */
public record FinancialAccount(
    UUID accountId, String name, String type, String subtype, String currencyCode) {
  public FinancialAccount {
    Objects.requireNonNull(accountId);
  }
}
