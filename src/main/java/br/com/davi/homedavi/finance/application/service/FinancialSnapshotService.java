package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.application.port.in.GetFinancialSnapshotUseCase;
import br.com.davi.homedavi.finance.application.port.in.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.in.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.out.FinancialSnapshotDataRepository;
import br.com.davi.homedavi.finance.domain.FinancialSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinancialSnapshotService implements GetFinancialSnapshotUseCase {
  private final ListFinancialAccountsUseCase accounts;
  private final GetAccountBalanceUseCase balances;
  private final FinancialSnapshotDataRepository snapshotData;

  public FinancialSnapshotService(
      ListFinancialAccountsUseCase accounts,
      GetAccountBalanceUseCase balances,
      FinancialSnapshotDataRepository snapshotData) {
    this.accounts = accounts;
    this.balances = balances;
    this.snapshotData = snapshotData;
  }

  @Override
  public FinancialSnapshot getFinancialSnapshot() {
    Instant asOf = Instant.now();
    var errors = new ArrayList<String>();
    var latestSync = java.util.Optional.<FinancialSnapshotDataRepository.SyncInfo>empty();
    var successfulSync = java.util.Optional.<FinancialSnapshotDataRepository.SyncInfo>empty();
    try {
      latestSync = snapshotData.findLatestSync();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível consultar o estado da sincronização: " + safeMessage(exception));
    }
    try {
      successfulSync = snapshotData.findLatestSuccessfulSync();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível consultar o horário da última sincronização: " + safeMessage(exception));
    }
    var lastSyncAt = successfulSync.map(FinancialSnapshotDataRepository.SyncInfo::completedAt)
        .orElse(null);

    String syncStatus =
        latestSync.map(FinancialSnapshotDataRepository.SyncInfo::status).map(status -> switch (status) {
          case "COMPLETED" -> "success";
          case "FAILED" -> "failed";
          case "STARTED" -> "in_progress";
          default -> "unknown";
        }).orElse("unknown");
    latestSync
        .filter(sync -> sync.errorMessage() != null && !sync.errorMessage().isBlank())
        .ifPresent(sync -> errors.add("Última sincronização falhou: " + sync.errorMessage()));
    if (lastSyncAt == null) errors.add("Ainda não há sincronização concluída registrada.");

    var accountSnapshots = new ArrayList<FinancialSnapshot.Account>();
    try {
      for (var account : accounts.listAccounts()) {
        try {
          var balance = balances.getAccountBalance(account.accountId());
          accountSnapshots.add(
              new FinancialSnapshot.Account(
                  account.accountId(),
                  account.name(),
                  balance.currentBalance(),
                  balance.availableBalance(),
                  balance.currencyCode()));
        } catch (RuntimeException exception) {
          errors.add(
              "Não foi possível consultar o saldo da conta "
                  + account.accountId()
                  + ": "
                  + safeMessage(exception));
        }
      }
    } catch (RuntimeException exception) {
      errors.add("Não foi possível listar as contas: " + safeMessage(exception));
    }

    Instant previousSyncAt = null;
    boolean transactionWindowAvailable = lastSyncAt != null;
    if (lastSyncAt != null) {
      try {
        previousSyncAt =
            snapshotData
                .findPreviousSuccessfulSync(lastSyncAt)
                .map(FinancialSnapshotDataRepository.SyncInfo::completedAt)
                .orElse(null);
      } catch (RuntimeException exception) {
        errors.add("Não foi possível calcular a janela de sincronização: " + safeMessage(exception));
        transactionWindowAvailable = false;
      }
    }
    List<FinancialSnapshot.Transaction> newTransactions = List.of();
    if (transactionWindowAvailable) {
      try {
        newTransactions = snapshotData.findTransactionsSyncedBetween(previousSyncAt, lastSyncAt);
      } catch (RuntimeException exception) {
        errors.add("Não foi possível listar as transações sincronizadas: " + safeMessage(exception));
      }
    }
    List<FinancialSnapshot.Transaction> pendingItems = List.of();
    try {
      pendingItems = snapshotData.findPendingTransactions();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível consultar transações pendentes: " + safeMessage(exception));
    }

    // O domínio atual não integra faturas; avisar evita que o agente interprete a lista vazia como
    // uma confirmação de inexistência de faturas.
    errors.add("Faturas em aberto não estão disponíveis nesta integração.");

    if (!errors.isEmpty() && "success".equals(syncStatus)) syncStatus = "partial";
    return new FinancialSnapshot(
        asOf,
        syncStatus,
        lastSyncAt,
        accountSnapshots,
        newTransactions,
        List.of(),
        pendingItems,
        errors);
  }

  private static String safeMessage(RuntimeException exception) {
    return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
  }
}
