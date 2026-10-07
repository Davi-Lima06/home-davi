package br.com.davi.homedavi.finance.domain.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Resposta consolidada para o agente consultar os dados financeiros em uma única chamada. */
public record FinancialSnapshot(
    Instant asOf,
    String syncStatus,
    Instant lastSyncAt,
    List<Account> accounts,
    List<Transaction> transactionsSinceLastSync,
    List<Invoice> openInvoices,
    List<Transaction> pendingItems,
    List<Connection> connections,
    List<Investment> investments,
    List<Loan> loans,
    List<String> errors) {
  public FinancialSnapshot {
    accounts = List.copyOf(accounts);
    transactionsSinceLastSync = List.copyOf(transactionsSinceLastSync);
    openInvoices = List.copyOf(openInvoices);
    pendingItems = List.copyOf(pendingItems);
    connections = List.copyOf(connections);
    investments = List.copyOf(investments);
    loans = List.copyOf(loans);
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

  public record Invoice(
      UUID id,
      UUID accountId,
      LocalDate dueDate,
      BigDecimal totalAmount,
      BigDecimal minimumPayment,
      String currency) {}

  public record Connection(
      UUID id,
      String connectorName,
      String status,
      String executionStatus,
      Instant lastUpdatedAt) {}

  public record Investment(UUID id, String name, String type, BigDecimal balance, String currency) {}

  public record Loan(UUID id, String contractNumber, BigDecimal outstandingBalance, String currency) {}
}
