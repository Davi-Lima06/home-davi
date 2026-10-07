package br.com.davi.homedavi.finance.application.port.out.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FinancialDataProvider {
  SyncedAccount fetchAccount(UUID accountId);

  SyncedTransaction fetchTransaction(UUID transactionId);

  /** createdAtFrom nulo busca todas as transações da conta. */
  List<SyncedTransaction> fetchCreatedTransactions(UUID accountId, Instant createdAtFrom);
}
