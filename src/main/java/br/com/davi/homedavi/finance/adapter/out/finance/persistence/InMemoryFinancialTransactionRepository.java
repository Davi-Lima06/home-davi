package br.com.davi.homedavi.finance.adapter.out.finance.persistence;

import br.com.davi.homedavi.finance.application.port.out.finance.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFinancialTransactionRepository implements FinancialTransactionRepository {
  private final ConcurrentHashMap<String, FinancialTransaction> transactions =
      new ConcurrentHashMap<>();

  @Override
  public FinancialTransaction save(FinancialTransaction transaction) {
    transactions.put(transaction.id().toString(), transaction);
    return transaction;
  }

  @Override
  public List<FinancialTransaction> findByAccountId(String accountId) {
    return transactions.values().stream()
        .filter(t -> t.accountId().equals(accountId))
        .sorted(Comparator.comparing(FinancialTransaction::occurredAt).reversed())
        .toList();
  }
}
