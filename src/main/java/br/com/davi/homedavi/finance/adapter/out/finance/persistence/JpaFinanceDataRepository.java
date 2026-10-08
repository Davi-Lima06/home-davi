package br.com.davi.homedavi.finance.adapter.out.finance.persistence;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.AccountEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.BillEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.CategoryEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.CategoryKind;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.FinanceTransactionType;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.LoanEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.TransactionEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.AccountJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.BillJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.CategoryJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.LoanJpaRepository;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.repository.TransactionJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.finance.FinanceDataRepository;
import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.SyncedTransaction;
import br.com.davi.homedavi.finance.domain.integration.TransactionChanges;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  private final LoanJpaRepository loanRepository;
  private final BillJpaRepository billRepository;

  public JpaFinanceDataRepository(
      AccountJpaRepository accountRepository,
      TransactionJpaRepository transactionRepository,
      CategoryJpaRepository categoryRepository,
      LoanJpaRepository loanRepository,
      BillJpaRepository billRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
    this.loanRepository = loanRepository;
    this.billRepository = billRepository;
  }

  @Override
  @Transactional
  public List<UUID> saveAccounts(UUID itemId, List<SyncedAccount> accounts) {
    var created = new ArrayList<UUID>();
    for (var account : accounts) {
      if (!accountRepository.existsById(account.id())) created.add(account.id());
      saveAccount(itemId, account);
    }
    return created;
  }

  @Override
  @Transactional
  public void saveLoans(UUID itemId, List<SyncedLoan> loans) {
    for (var loan : loans) {
      var entity = loanRepository.findById(loan.id()).orElseGet(() -> new LoanEntity(loan.id(), itemId));
      entity.setContractNumber(loan.contractNumber());
      entity.setOutstandingBalance(loan.outstandingBalance());
      entity.setCurrencyCode(loan.currencyCode());
      entity.setRawData(loan.rawDataJson());
      loanRepository.save(entity);
    }
  }

  @Override
  @Transactional
  public void saveBills(List<SyncedBill> bills) {
    for (var bill : bills) {
      var entity =
          billRepository.findById(bill.id()).orElseGet(() -> new BillEntity(bill.id(), bill.accountId()));
      entity.setDueDate(bill.dueDate());
      entity.setTotalAmount(bill.totalAmount());
      entity.setMinimumPayment(bill.minimumPayment());
      entity.setCurrencyCode(bill.currencyCode());
      entity.setRawData(bill.rawDataJson());
      billRepository.save(entity);
    }
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
      case TRANSFER -> FinanceTransactionType.TRANSFER;
      case UNKNOWN -> FinanceTransactionType.UNKNOWN;
    };
  }
}
