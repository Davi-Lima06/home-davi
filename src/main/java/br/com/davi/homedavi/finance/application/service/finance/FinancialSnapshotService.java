package br.com.davi.homedavi.finance.application.service.finance;

import br.com.davi.homedavi.finance.application.port.in.finance.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.in.finance.GetFinancialSnapshotUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListBankConnectionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListCreditCardBillsUseCase;
import br.com.davi.homedavi.finance.application.port.in.finance.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListInvestmentsUseCase;
import br.com.davi.homedavi.finance.application.port.in.integration.ListLoansUseCase;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialSnapshotDataRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialAccount;
import br.com.davi.homedavi.finance.domain.finance.FinancialSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FinancialSnapshotService implements GetFinancialSnapshotUseCase {
  private final ListFinancialAccountsUseCase accounts;
  private final GetAccountBalanceUseCase balances;
  private final ListCreditCardBillsUseCase bills;
  private final ListBankConnectionsUseCase bankConnections;
  private final ListInvestmentsUseCase investmentsQuery;
  private final ListLoansUseCase loansQuery;
  private final FinancialSnapshotDataRepository snapshotData;

  public FinancialSnapshotService(
      ListFinancialAccountsUseCase accounts,
      GetAccountBalanceUseCase balances,
      ListCreditCardBillsUseCase bills,
      ListBankConnectionsUseCase bankConnections,
      ListInvestmentsUseCase investmentsQuery,
      ListLoansUseCase loansQuery,
      FinancialSnapshotDataRepository snapshotData) {
    this.accounts = accounts;
    this.balances = balances;
    this.bills = bills;
    this.bankConnections = bankConnections;
    this.investmentsQuery = investmentsQuery;
    this.loansQuery = loansQuery;
    this.snapshotData = snapshotData;
  }

  @Override
  public FinancialSnapshot getFinancialSnapshot() {
    Instant asOf = Instant.now();
    var errors = new ArrayList<String>();
    var latestSync = Optional.<FinancialSnapshotDataRepository.SyncInfo>empty();
    var successfulSync = Optional.<FinancialSnapshotDataRepository.SyncInfo>empty();
    try {
      latestSync = snapshotData.findLatestSync();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível consultar o estado da sincronização: " + safeMessage(exception));
    }
    try {
      successfulSync = snapshotData.findLatestSuccessfulSync();
    } catch (RuntimeException exception) {
      errors.add(
          "Não foi possível consultar o horário da última sincronização: " + safeMessage(exception));
    }
    var lastSyncAt =
        successfulSync.map(FinancialSnapshotDataRepository.SyncInfo::completedAt).orElse(null);

    String syncStatus =
        latestSync
            .map(FinancialSnapshotDataRepository.SyncInfo::status)
            .map(
                status ->
                    switch (status) {
                      case "COMPLETED" -> "success";
                      case "FAILED" -> "failed";
                      case "STARTED" -> "in_progress";
                      default -> "unknown";
                    })
            .orElse("unknown");
    latestSync
        .filter(sync -> sync.errorMessage() != null && !sync.errorMessage().isBlank())
        .ifPresent(sync -> errors.add("Última sincronização falhou: " + sync.errorMessage()));
    if (lastSyncAt == null) errors.add("Ainda não há sincronização concluída registrada.");

    List<FinancialAccount> financialAccounts = List.of();
    try {
      financialAccounts = accounts.listAccounts();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível listar as contas: " + safeMessage(exception));
    }

    var accountSnapshots = new ArrayList<FinancialSnapshot.Account>();
    var openInvoices = new ArrayList<FinancialSnapshot.Invoice>();
    for (var account : financialAccounts) {
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
      if (isCreditCard(account)) {
        try {
          for (var bill : bills.listBills(account.accountId())) {
            openInvoices.add(
                new FinancialSnapshot.Invoice(
                    bill.id(),
                    bill.accountId(),
                    bill.dueDate(),
                    bill.totalAmount(),
                    bill.minimumPayment(),
                    bill.currencyCode()));
          }
        } catch (RuntimeException exception) {
          errors.add(
              "Não foi possível consultar as faturas da conta "
                  + account.accountId()
                  + ": "
                  + safeMessage(exception));
        }
      }
    }

    // Os itemId vêm do banco (finance.accounts), então investimentos/empréstimos não dependem do
    // /v2/items, que é opt-in na Pluggy.
    List<UUID> itemIds = List.of();
    try {
      itemIds = snapshotData.findActiveItemIds();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível descobrir as conexões sincronizadas: " + safeMessage(exception));
    }

    // A listagem de conexões (estado de cada item) depende do /v2/items, que é opt-in; se não
    // estiver habilitado, seguimos sem essa seção — investimentos e empréstimos não são afetados.
    var connections = new ArrayList<FinancialSnapshot.Connection>();
    try {
      for (var item : bankConnections.listItems()) {
        connections.add(
            new FinancialSnapshot.Connection(
                item.id(),
                item.connectorName(),
                item.status(),
                item.executionStatus(),
                item.lastUpdatedAt()));
      }
    } catch (RuntimeException exception) {
      errors.add(
          "Estado das conexões bancárias indisponível (o endpoint /v2/items é opt-in na Pluggy): "
              + safeMessage(exception));
    }

    var investments = new ArrayList<FinancialSnapshot.Investment>();
    var loans = new ArrayList<FinancialSnapshot.Loan>();
    for (var itemId : itemIds) {
      try {
        for (var investment : investmentsQuery.listInvestments(itemId)) {
          investments.add(
              new FinancialSnapshot.Investment(
                  investment.id(),
                  investment.name(),
                  investment.type(),
                  investment.balance(),
                  investment.currencyCode()));
        }
      } catch (RuntimeException exception) {
        errors.add(
            "Não foi possível listar os investimentos do item "
                + itemId
                + ": "
                + safeMessage(exception));
      }
      try {
        for (var loan : loansQuery.listLoans(itemId)) {
          loans.add(
              new FinancialSnapshot.Loan(
                  loan.id(), loan.contractNumber(), loan.outstandingBalance(), loan.currencyCode()));
        }
      } catch (RuntimeException exception) {
        errors.add(
            "Não foi possível listar os empréstimos do item "
                + itemId
                + ": "
                + safeMessage(exception));
      }
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
        errors.add(
            "Não foi possível calcular a janela de sincronização: " + safeMessage(exception));
        transactionWindowAvailable = false;
      }
    }
    List<FinancialSnapshot.Transaction> newTransactions = List.of();
    if (transactionWindowAvailable) {
      try {
        newTransactions = snapshotData.findTransactionsSyncedBetween(previousSyncAt, lastSyncAt);
      } catch (RuntimeException exception) {
        errors.add(
            "Não foi possível listar as transações sincronizadas: " + safeMessage(exception));
      }
    }
    List<FinancialSnapshot.Transaction> pendingItems = List.of();
    try {
      pendingItems = snapshotData.findPendingTransactions();
    } catch (RuntimeException exception) {
      errors.add("Não foi possível consultar transações pendentes: " + safeMessage(exception));
    }

    if (!errors.isEmpty() && "success".equals(syncStatus)) syncStatus = "partial";
    return new FinancialSnapshot(
        asOf,
        syncStatus,
        lastSyncAt,
        accountSnapshots,
        newTransactions,
        openInvoices,
        pendingItems,
        connections,
        investments,
        loans,
        errors);
  }

  private static boolean isCreditCard(FinancialAccount account) {
    return account.type() != null && account.type().equalsIgnoreCase("CREDIT");
  }

  private static String safeMessage(RuntimeException exception) {
    return exception.getMessage() == null
        ? exception.getClass().getSimpleName()
        : exception.getMessage();
  }
}
