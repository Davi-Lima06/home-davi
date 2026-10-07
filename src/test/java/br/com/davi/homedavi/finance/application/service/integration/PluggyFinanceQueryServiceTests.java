package br.com.davi.homedavi.finance.application.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.out.integration.FinancialQueryProvider;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.integration.TransactionPage;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PluggyFinanceQueryServiceTests {
  private static final UUID ITEM = UUID.randomUUID();
  private static final UUID ACCOUNT = UUID.randomUUID();

  private final RecordingProvider provider = new RecordingProvider();
  private final PluggyFinanceQueryService service = new PluggyFinanceQueryService(provider);

  @Test
  void delegatesListsToProvider() {
    assertEquals(1, service.listAccounts(ITEM).size());
    assertEquals(ITEM, provider.itemId);

    assertEquals(1, service.listBills(ACCOUNT).size());
    assertEquals(ACCOUNT, provider.accountId);

    assertEquals(1, service.listItems().size());
    assertEquals(1, service.listInvestments(ITEM).size());
    assertEquals(1, service.listLoans(ITEM).size());
  }

  @Test
  void passesCursorThroughAndReportsHasMore() {
    var page = service.listTransactions(ACCOUNT, "cursor-1", 50);
    assertEquals("cursor-1", provider.cursor);
    assertEquals(50, provider.pageSize);
    assertTrue(page.hasMore());
    assertEquals("cursor-2", page.nextCursor());
  }

  @Test
  void normalizesPageSize() {
    service.listTransactions(ACCOUNT, null, null);
    assertEquals(100, provider.pageSize); // default

    service.listTransactions(ACCOUNT, null, 0);
    assertEquals(100, provider.pageSize); // inválido -> default

    service.listTransactions(ACCOUNT, null, 9000);
    assertEquals(500, provider.pageSize); // teto

    var lastPage = service.listTransactions(ACCOUNT, null, 10);
    assertFalse(lastPage.hasMore());
  }

  private static final class RecordingProvider implements FinancialQueryProvider {
    private UUID itemId;
    private UUID accountId;
    private String cursor;
    private int pageSize;

    @Override
    public List<SyncedAccount> fetchAccounts(UUID itemId) {
      this.itemId = itemId;
      return List.of(
          new SyncedAccount(ACCOUNT, "Conta", "BANK", null, null, "BRL", BigDecimal.TEN, BigDecimal.TEN, null, null, "{}"));
    }

    @Override
    public TransactionPage fetchTransactionsPage(UUID accountId, String cursor, int pageSize) {
      this.accountId = accountId;
      this.cursor = cursor;
      this.pageSize = pageSize;
      var tx =
          new SyncedTransaction(
              UUID.randomUUID(), accountId, "Mercado", null, null, new BigDecimal("12.30"), "BRL",
              TransactionType.EXPENSE, "POSTED", Instant.now(), false, "{}");
      // Simula "última página" quando o cursor vem nulo.
      return new TransactionPage(List.of(tx), cursor == null ? null : "cursor-2");
    }

    @Override
    public List<SyncedBill> fetchBills(UUID accountId) {
      this.accountId = accountId;
      return List.of(new SyncedBill(UUID.randomUUID(), accountId, null, BigDecimal.TEN, BigDecimal.ONE, "BRL", "{}"));
    }

    @Override
    public List<SyncedItem> fetchItems() {
      return List.of(new SyncedItem(ITEM, "Nubank", "UPDATED", "SUCCESS", Instant.now(), "{}"));
    }

    @Override
    public List<SyncedInvestment> fetchInvestments(UUID itemId) {
      this.itemId = itemId;
      return List.of(new SyncedInvestment(UUID.randomUUID(), "Tesouro", "FIXED_INCOME", BigDecimal.TEN, "BRL", "{}"));
    }

    @Override
    public List<SyncedLoan> fetchLoans(UUID itemId) {
      this.itemId = itemId;
      return List.of(new SyncedLoan(UUID.randomUUID(), "CONTRACT-1", BigDecimal.TEN, "BRL", "{}"));
    }
  }
}
