package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.integration.TransactionChanges;

public interface FinanceDataRepository {
  void saveTransactionChanges(TransactionChanges changes);
}
