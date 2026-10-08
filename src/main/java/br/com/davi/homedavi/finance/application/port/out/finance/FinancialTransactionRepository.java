package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import java.util.List;

public interface FinancialTransactionRepository {
  FinancialTransaction save(FinancialTransaction transaction);

  List<FinancialTransaction> findAll();
}
