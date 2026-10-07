package br.com.davi.homedavi.finance.application.service.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.in.finance.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListBankConnectionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListCreditCardBillsUseCase;
import br.com.davi.homedavi.finance.application.port.in.finance.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListInvestmentsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListLoansUseCase;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialSnapshotDataRepository;
import br.com.davi.homedavi.finance.domain.finance.AccountBalance;
import br.com.davi.homedavi.finance.domain.finance.FinancialAccount;
import br.com.davi.homedavi.finance.domain.finance.FinancialSnapshot;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialSnapshotServiceTests {

  private static final ListCreditCardBillsUseCase NO_BILLS = accountId -> List.of();
  private static final ListBankConnectionsUseCase NO_CONNECTIONS = List::of;
  private static final ListInvestmentsUseCase NO_INVESTMENTS = itemId -> List.of();
  private static final ListLoansUseCase NO_LOANS = itemId -> List.of();

  @Test
  void consolidatesInvoicesConnectionsInvestmentsAndLoans() {
    UUID creditAccount = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    Instant completedAt = Instant.parse("2026-10-06T11:59:42Z");

    ListFinancialAccountsUseCase accounts =
        () -> List.of(new FinancialAccount(creditAccount, "Cartão", "CREDIT", "CREDIT_CARD", "BRL"));
    GetAccountBalanceUseCase balances =
        id -> new AccountBalance(id, "Cartão", "BRL", new BigDecimal("-1200.00"), null, new BigDecimal("5000.00"));
    ListCreditCardBillsUseCase bills =
        id ->
            List.of(
                new SyncedBill(
                    UUID.randomUUID(), id, LocalDate.parse("2026-11-10"),
                    new BigDecimal("1200.00"), new BigDecimal("300.00"), "BRL", "{}"));
    ListBankConnectionsUseCase connections =
        () -> List.of(new SyncedItem(itemId, "Nubank", "UPDATED", "SUCCESS", completedAt, "{}"));
    ListInvestmentsUseCase investments =
        id ->
            List.of(
                new SyncedInvestment(
                    UUID.randomUUID(), "Tesouro Selic", "FIXED_INCOME", new BigDecimal("10000.00"), "BRL", "{}"));
    ListLoansUseCase loans =
        id -> List.of(new SyncedLoan(UUID.randomUUID(), "CT-1", new BigDecimal("2500.00"), "BRL", "{}"));
    FinancialSnapshotDataRepository data =
        new InMemorySnapshotData(
            List.of(itemId),
            Optional.of(new FinancialSnapshotDataRepository.SyncInfo("COMPLETED", completedAt, null)),
            Optional.of(new FinancialSnapshotDataRepository.SyncInfo("COMPLETED", completedAt, null)));

    var snapshot =
        new FinancialSnapshotService(accounts, balances, bills, connections, investments, loans, data)
            .getFinancialSnapshot();

    assertEquals("success", snapshot.syncStatus());
    assertTrue(snapshot.errors().isEmpty(), () -> "erros inesperados: " + snapshot.errors());
    assertEquals(1, snapshot.openInvoices().size());
    assertEquals(LocalDate.parse("2026-11-10"), snapshot.openInvoices().getFirst().dueDate());
    assertEquals(1, snapshot.connections().size());
    assertEquals("Nubank", snapshot.connections().getFirst().connectorName());
    assertEquals(1, snapshot.investments().size());
    assertEquals(1, snapshot.loans().size());
  }

  @Test
  void degradesGracefullyWhenProvidersFail() {
    UUID accountId = UUID.randomUUID();
    ListFinancialAccountsUseCase accounts =
        () -> List.of(new FinancialAccount(accountId, "Conta", "BANK", null, "BRL"));
    GetAccountBalanceUseCase balances =
        id -> {
          throw new IllegalStateException("Pluggy offline");
        };
    ListBankConnectionsUseCase connections =
        () -> {
          throw new IllegalStateException("items opt-in desabilitado");
        };
    FinancialSnapshotDataRepository data =
        new InMemorySnapshotData(List.of(), Optional.empty(), Optional.empty());

    var snapshot =
        new FinancialSnapshotService(
                accounts, balances, NO_BILLS, connections, NO_INVESTMENTS, NO_LOANS, data)
            .getFinancialSnapshot();

    assertEquals("unknown", snapshot.syncStatus());
    assertNull(snapshot.lastSyncAt());
    assertTrue(snapshot.accounts().isEmpty());
    assertTrue(snapshot.connections().isEmpty());
    assertTrue(snapshot.investments().isEmpty());
    assertTrue(snapshot.errors().stream().anyMatch(error -> error.contains("Pluggy offline")));
    assertTrue(snapshot.errors().stream().anyMatch(error -> error.contains("conexões bancárias")));
  }

  @Test
  void doesNotFetchBillsForNonCreditAccounts() {
    UUID bankAccount = UUID.randomUUID();
    ListFinancialAccountsUseCase accounts =
        () -> List.of(new FinancialAccount(bankAccount, "Conta Corrente", "BANK", null, "BRL"));
    GetAccountBalanceUseCase balances =
        id -> new AccountBalance(id, "Conta Corrente", "BRL", BigDecimal.TEN, BigDecimal.TEN, null);
    ListCreditCardBillsUseCase bills =
        id -> {
          throw new AssertionError("não deveria consultar faturas de conta não-cartão");
        };
    FinancialSnapshotDataRepository data =
        new InMemorySnapshotData(List.of(), Optional.empty(), Optional.empty());

    var snapshot =
        new FinancialSnapshotService(
                accounts, balances, bills, NO_CONNECTIONS, NO_INVESTMENTS, NO_LOANS, data)
            .getFinancialSnapshot();

    assertTrue(snapshot.openInvoices().isEmpty());
    assertEquals(1, snapshot.accounts().size());
  }

  private record InMemorySnapshotData(
      List<UUID> itemIds, Optional<SyncInfo> latest, Optional<SyncInfo> latestSuccessful)
      implements FinancialSnapshotDataRepository {
    @Override
    public List<UUID> findActiveItemIds() {
      return itemIds;
    }

    @Override
    public Optional<SyncInfo> findLatestSync() {
      return latest;
    }

    @Override
    public Optional<SyncInfo> findLatestSuccessfulSync() {
      return latestSuccessful;
    }

    @Override
    public Optional<SyncInfo> findPreviousSuccessfulSync(Instant before) {
      return Optional.empty();
    }

    @Override
    public List<FinancialSnapshot.Transaction> findTransactionsSyncedBetween(
        Instant after, Instant through) {
      return List.of();
    }

    @Override
    public List<FinancialSnapshot.Transaction> findPendingTransactions() {
      return List.of();
    }
  }
}
