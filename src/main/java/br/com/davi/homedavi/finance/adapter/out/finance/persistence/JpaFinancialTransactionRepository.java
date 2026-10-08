package br.com.davi.homedavi.finance.adapter.out.finance.persistence;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.AccountEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.FinanceTransactionType;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.TransactionEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.AccountJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.TransactionJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Lê e grava transações na tabela finance.transactions. */
@Repository
public class JpaFinancialTransactionRepository implements FinancialTransactionRepository {
  private final TransactionJpaRepository transactions;
  private final AccountJpaRepository accounts;

  public JpaFinancialTransactionRepository(
      TransactionJpaRepository transactions, AccountJpaRepository accounts) {
    this.transactions = transactions;
    this.accounts = accounts;
  }

  @Override
  @Transactional
  public FinancialTransaction save(FinancialTransaction transaction) {
    UUID accountId = UUID.fromString(transaction.accountId());
    AccountEntity account =
        accounts
            .findById(accountId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conta não encontrada em finance.accounts: " + accountId));
    var entity =
        new TransactionEntity(
            transaction.id(),
            account.getItemId(),
            account,
            transaction.description(),
            transaction.amount(),
            toEntityType(transaction.type()),
            transaction.occurredAt());
    entity.setCurrencyCode(transaction.currency());
    return toDomain(transactions.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinancialTransaction> findByAccountType(
      String accountType, Instant fromInclusive, Instant toExclusive) {
    return transactions.findByAccountTypeAndWindow(accountType, fromInclusive, toExclusive).stream()
        .map(JpaFinancialTransactionRepository::toDomain)
        .toList();
  }

  static FinancialTransaction toDomain(TransactionEntity entity) {
    return new FinancialTransaction(
        entity.getId(),
        entity.getAccount().getId().toString(),
        entity.getDescription(),
        entity.getAmount(),
        entity.getCurrencyCode(),
        toDomainType(entity.getTransactionType()),
        entity.getOccurredAt(),
        // A entidade não tem coluna externalId; o próprio id é o id da transação na Pluggy.
        entity.getId().toString());
  }

  private static FinanceTransactionType toEntityType(TransactionType type) {
    return switch (type) {
      case INCOME -> FinanceTransactionType.INCOME;
      case EXPENSE -> FinanceTransactionType.EXPENSE;
      case TRANSFER -> FinanceTransactionType.TRANSFER;
      case UNKNOWN -> FinanceTransactionType.UNKNOWN;
    };
  }

  private static TransactionType toDomainType(FinanceTransactionType type) {
    return switch (type) {
      case INCOME -> TransactionType.INCOME;
      case EXPENSE -> TransactionType.EXPENSE;
      case TRANSFER -> TransactionType.TRANSFER;
      case UNKNOWN -> TransactionType.UNKNOWN;
    };
  }
}
