package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.application.port.in.ListAccountTransactionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListBankConnectionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListConnectedAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListCreditCardBillsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListInvestmentsUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListLoansUseCase;
import br.com.davi.homedavi.finance.application.port.out.FinancialQueryProvider;
import br.com.davi.homedavi.finance.domain.SyncedAccount;
import br.com.davi.homedavi.finance.domain.SyncedBill;
import br.com.davi.homedavi.finance.domain.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.SyncedItem;
import br.com.davi.homedavi.finance.domain.SyncedLoan;
import br.com.davi.homedavi.finance.domain.TransactionPage;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Consultas de leitura ao vivo no Pluggy, expostas pelas tools MCP do tópico finance. */
@Service
public class PluggyFinanceQueryService
    implements ListConnectedAccountsUseCase,
        ListAccountTransactionsUseCase,
        ListCreditCardBillsUseCase,
        ListBankConnectionsUseCase,
        ListInvestmentsUseCase,
        ListLoansUseCase {
  private static final Logger log = LoggerFactory.getLogger(PluggyFinanceQueryService.class);
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int MAX_PAGE_SIZE = 500;

  private final FinancialQueryProvider queryProvider;

  public PluggyFinanceQueryService(FinancialQueryProvider queryProvider) {
    this.queryProvider = queryProvider;
  }

  @Override
  public List<SyncedAccount> listAccounts(UUID itemId) {
    log.info("Listing accounts from Pluggy: itemId={}", itemId);
    var accounts = queryProvider.fetchAccounts(itemId);
    log.info("Accounts listed from Pluggy: itemId={}, count={}", itemId, accounts.size());
    return accounts;
  }

  @Override
  public TransactionPage listTransactions(UUID accountId, String cursor, Integer pageSize) {
    int size = normalizePageSize(pageSize);
    log.info(
        "Listing transactions from Pluggy v2: accountId={}, pageSize={}, hasCursor={}",
        accountId,
        size,
        cursor != null && !cursor.isBlank());
    var page = queryProvider.fetchTransactionsPage(accountId, cursor, size);
    log.info(
        "Transactions page fetched: accountId={}, count={}, hasMore={}",
        accountId,
        page.transactions().size(),
        page.hasMore());
    return page;
  }

  @Override
  public List<SyncedBill> listBills(UUID accountId) {
    log.info("Listing credit card bills from Pluggy: accountId={}", accountId);
    var bills = queryProvider.fetchBills(accountId);
    log.info("Bills listed from Pluggy: accountId={}, count={}", accountId, bills.size());
    return bills;
  }

  @Override
  public List<SyncedItem> listItems() {
    log.info("Listing bank connections from Pluggy v2");
    var items = queryProvider.fetchItems();
    log.info("Bank connections listed from Pluggy: count={}", items.size());
    return items;
  }

  @Override
  public List<SyncedInvestment> listInvestments(UUID itemId) {
    log.info("Listing investments from Pluggy: itemId={}", itemId);
    var investments = queryProvider.fetchInvestments(itemId);
    log.info("Investments listed from Pluggy: itemId={}, count={}", itemId, investments.size());
    return investments;
  }

  @Override
  public List<SyncedLoan> listLoans(UUID itemId) {
    log.info("Listing loans from Pluggy: itemId={}", itemId);
    var loans = queryProvider.fetchLoans(itemId);
    log.info("Loans listed from Pluggy: itemId={}, count={}", itemId, loans.size());
    return loans;
  }

  private static int normalizePageSize(Integer pageSize) {
    if (pageSize == null || pageSize <= 0) return DEFAULT_PAGE_SIZE;
    return Math.min(pageSize, MAX_PAGE_SIZE);
  }
}
