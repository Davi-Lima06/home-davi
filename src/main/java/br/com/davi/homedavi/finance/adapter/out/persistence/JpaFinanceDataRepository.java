package br.com.davi.homedavi.finance.adapter.out.persistence;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.AccountEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.CategoryEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.CategoryKind;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.FinanceTransactionType;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.TransactionEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.AccountJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.CategoryJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.TransactionJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.FinanceDataRepository;
import br.com.davi.homedavi.finance.domain.SyncedAccount;
import br.com.davi.homedavi.finance.domain.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.TransactionChanges;
import br.com.davi.homedavi.finance.domain.TransactionType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFinanceDataRepository implements FinanceDataRepository {
  private static final Logger log = LoggerFactory.getLogger(JpaFinanceDataRepository.class);

  private final AccountJpaRepository accountRepository;
  private final TransactionJpaRepository transactionRepository;
  private final CategoryJpaRepository categoryRepository;

  public JpaFinanceDataRepository(
      AccountJpaRepository accountRepository,
      TransactionJpaRepository transactionRepository,
      CategoryJpaRepository categoryRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
  }

  @Override
  @Transactional
  public void saveTransactionChanges(TransactionChanges changes) {
    var accounts = new HashMap<UUID, AccountEntity>();
    changes
        .accounts()
        .forEach(account -> accounts.put(account.id(), saveAccount(changes.itemId(), account)));

    var existing =
        transactionRepository
            .findAllById(changes.transactions().stream().map(SyncedTransaction::id).toList())
            .stream()
            .collect(Collectors.toMap(TransactionEntity::getId, Function.identity()));
    var categories = new HashMap<String, CategoryEntity>();
    for (var transaction : changes.transactions()) {
      var entity = existing.get(transaction.id());
      if (entity == null) {
        var account =
            accounts.computeIfAbsent(transaction.accountId(), accountRepository::getReferenceById);
        entity =
            new TransactionEntity(
                transaction.id(),
                changes.itemId(),
                account,
                transaction.description(),
                transaction.amount(),
                toEntityType(transaction.type()),
                transaction.occurredAt());
      }
      applyTransaction(entity, transaction, categories);
      if (!existing.containsKey(transaction.id())) transactionRepository.save(entity);
    }

    transactionRepository.deleteAllById(changes.deletedTransactionIds());
    log.debug(
        "[5/7] Upsert finished: itemId={}, newTransactions={}, updatedTransactions={}",
        changes.itemId(),
        changes.transactions().size() - existing.size(),
        existing.size());
  }

  private AccountEntity saveAccount(UUID itemId, SyncedAccount account) {
    var entity =
        accountRepository
            .findById(account.id())
            .orElseGet(() -> new AccountEntity(account.id(), itemId, account.name()));
    entity.setName(account.name());
    entity.setType(account.type());
    entity.setSubtype(account.subtype());
    entity.setNumberMasked(account.numberMasked());
    entity.setCurrencyCode(account.currencyCode());
    entity.setCurrentBalance(account.currentBalance());
    entity.setAvailableBalance(account.availableBalance());
    entity.setCreditLimit(account.creditLimit());
    entity.setStatus(account.status());
    entity.setRawData(account.rawDataJson());
    return accountRepository.save(entity);
  }

  private void applyTransaction(
      TransactionEntity entity,
      SyncedTransaction transaction,
      Map<String, CategoryEntity> categories) {
    entity.setDescription(transaction.description());
    entity.setMerchantName(transaction.merchantName());
    entity.setAmount(transaction.amount());
    entity.setCurrencyCode(transaction.currencyCode());
    entity.setTransactionType(toEntityType(transaction.type()));
    entity.setStatus(transaction.status());
    entity.setOccurredAt(transaction.occurredAt());
    entity.setPending(transaction.pending());
    entity.setRawData(transaction.rawDataJson());
    // Preserva a categoria quando ela já foi definida (inclusive manualmente).
    if (entity.getCategory() == null)
      entity.setCategory(findOrCreateCategory(transaction, categories));
  }

  private CategoryEntity findOrCreateCategory(
      SyncedTransaction transaction, Map<String, CategoryEntity> categories) {
    if (transaction.categoryName() == null) return null;
    var kind =
        transaction.type() == TransactionType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
    return categories.computeIfAbsent(
        kind + ":" + transaction.categoryName(),
        key ->
            categoryRepository
                .findByNameAndKind(transaction.categoryName(), kind)
                .orElseGet(
                    () ->
                        categoryRepository.save(
                            new CategoryEntity(transaction.categoryName(), kind))));
  }

  private static FinanceTransactionType toEntityType(TransactionType type) {
    return switch (type) {
      case INCOME -> FinanceTransactionType.INCOME;
      case EXPENSE -> FinanceTransactionType.EXPENSE;
    };
  }
}
