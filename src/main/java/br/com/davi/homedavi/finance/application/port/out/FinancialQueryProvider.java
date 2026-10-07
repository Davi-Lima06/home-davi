package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.SyncedAccount;
import br.com.davi.homedavi.finance.domain.SyncedBill;
import br.com.davi.homedavi.finance.domain.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.SyncedItem;
import br.com.davi.homedavi.finance.domain.SyncedLoan;
import br.com.davi.homedavi.finance.domain.TransactionPage;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de leitura ao vivo no Pluggy, usadas pelas tools MCP. Separada de {@link
 * FinancialDataProvider} (usado na sincronização via webhook) para manter as portas enxutas.
 */
public interface FinancialQueryProvider {
  List<SyncedAccount> fetchAccounts(UUID itemId);

  /** Uma página da API v2 de transações; cursor nulo busca a primeira. */
  TransactionPage fetchTransactionsPage(UUID accountId, String cursor, int pageSize);

  List<SyncedBill> fetchBills(UUID accountId);

  List<SyncedItem> fetchItems();

  List<SyncedInvestment> fetchInvestments(UUID itemId);

  List<SyncedLoan> fetchLoans(UUID itemId);
}
