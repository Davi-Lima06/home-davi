package br.com.davi.homedavi.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import br.com.davi.homedavi.finance.application.port.in.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.out.FinancialSnapshotDataRepository;
import br.com.davi.homedavi.finance.domain.AccountBalance;
import br.com.davi.homedavi.finance.domain.FinancialAccount;
import br.com.davi.homedavi.finance.domain.FinancialSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialSnapshotServiceTests {
  @Test
  void returnsConsolidatedDataAndReportsUnavailableInvoices() {
    UUID accountId = UUID.randomUUID();
    Instant completedAt = Instant.parse("2026-10-06T11:59:42Z");
    ListFinancialAccountsUseCase accounts =
        () -> List.of(new FinancialAccount(accountId, "Conta Corrente", "BANK", null, "BRL"));
    GetAccountBalanceUseCase balances =
        id ->
            new AccountBalance(
                id,
                "Conta Corrente",
                "BRL",
                new BigDecimal("36471.90"),
                new BigDecimal("36471.90"),
                null);
    FinancialSnapshotDataRepository data =
        new InMemorySnapshotData(
            Optional.of(new FinancialSnapshotDataRepository.SyncInfo("COMPLETED", completedAt, null)),
            Optional.of(new FinancialSnapshotDataRepository.SyncInfo("COMPLETED", completedAt, null)));

    var snapshot = new FinancialSnapshotService(accounts, balances, data).getFinancialSnapshot();

    assertEquals("partial", snapshot.syncStatus());
    assertEquals(completedAt, snapshot.lastSyncAt());
    assertEquals(new BigDecimal("36471.90"), snapshot.accounts().getFirst().currentBalance());
    assertTrue(snapshot.openInvoices().isEmpty());
    assertTrue(snapshot.errors().stream().anyMatch(error -> error.contains("Faturas")));
    assertNull(snapshot.transactionsSinceLastSync().stream().findFirst().orElse(null));
  }

  @Test
  void reportsFailedBalanceAndDoesNotClaimCompleteSnapshot() {
    UUID accountId = UUID.randomUUID();
    ListFinancialAccountsUseCase accounts =
        () -> List.of(new FinancialAccount(accountId, "Conta", "BANK", null, "BRL"));
    GetAccountBalanceUseCase balances =
        id -> {
          throw new IllegalStateException("Pluggy offline");
        };
    FinancialSnapshotDataRepository data =
        new InMemorySnapshotData(Optional.empty(), Optional.empty());

    var snapshot = new FinancialSnapshotService(accounts, balances, data).getFinancialSnapshot();

    assertEquals("unknown", snapshot.syncStatus());
    assertNull(snapshot.lastSyncAt());
    assertTrue(snapshot.accounts().isEmpty());
    assertTrue(snapshot.errors().stream().anyMatch(error -> error.contains("Pluggy offline")));
  }

  private record InMemorySnapshotData(
      Optional<SyncInfo> latest, Optional<SyncInfo> latestSuccessful)
      implements FinancialSnapshotDataRepository {
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
