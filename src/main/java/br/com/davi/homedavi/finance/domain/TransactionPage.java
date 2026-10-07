package br.com.davi.homedavi.finance.domain;

import java.util.List;

/**
 * Página de transações da API v2 do Pluggy (paginação por cursor). nextCursor nulo indica a última
 * página.
 */
public record TransactionPage(List<SyncedTransaction> transactions, String nextCursor) {
  public TransactionPage {
    transactions = transactions == null ? List.of() : List.copyOf(transactions);
  }

  public boolean hasMore() {
    return nextCursor != null && !nextCursor.isBlank();
  }
}
