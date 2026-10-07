package br.com.davi.homedavi.finance.domain.integration;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Alterações de um item (conexão bancária) a serem aplicadas no schema finance de forma atômica.
 */
public record TransactionChanges(
    UUID itemId,
    List<SyncedAccount> accounts,
    List<SyncedTransaction> transactions,
    List<UUID> deletedTransactionIds) {
  public TransactionChanges {
    Objects.requireNonNull(itemId);
    accounts = List.copyOf(accounts);
    transactions = List.copyOf(transactions);
    deletedTransactionIds = List.copyOf(deletedTransactionIds);
  }
}
