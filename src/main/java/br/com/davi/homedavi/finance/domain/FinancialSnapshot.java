package br.com.davi.homedavi.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resposta consolidada para o agente consultar os dados financeiros em uma única chamada. */
public record FinancialSnapshot(
    Instant asOf,
    String syncStatus,
    Instant lastSyncAt,
    List<Account> accounts,
    List<Transaction> transactionsSinceLastSync,
    List<Object> openInvoices,
    List<Transaction> pendingItems,
    List<String> errors) {
  public FinancialSnapshot {
    accounts = List.copyOf(accounts);
    transactionsSinceLastSync = List.copyOf(transactionsSinceLastSync);
    openInvoices = List.copyOf(openInvoices);
    pendingItems = List.copyOf(pendingItems);
    errors = List.copyOf(errors);
  }

  public record Account(
      UUID id,
      String name,
      BigDecimal currentBalance,
      BigDecimal availableBalance,
      String currency) {}

  public record Transaction(
      UUID id,
      UUID accountId,
      String description,
      BigDecimal amount,
      String currency,
      String type,
      String status,
      Instant occurredAt,
      Instant syncedAt) {}
}
