package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import java.util.List;

public interface FinancialTransactionRepository {
  FinancialTransaction save(FinancialTransaction transaction);

  List<FinancialTransaction> findByAccountId(String accountId);
}
