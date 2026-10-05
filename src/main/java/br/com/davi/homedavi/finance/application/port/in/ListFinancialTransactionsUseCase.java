package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import java.util.List;

public interface ListFinancialTransactionsUseCase {
  List<FinancialTransaction> listByAccount(String accountId);
}
