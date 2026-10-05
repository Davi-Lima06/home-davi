package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.TransactionChanges;

public interface FinanceDataRepository {
  void saveTransactionChanges(TransactionChanges changes);
}
