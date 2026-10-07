package br.com.davi.homedavi.finance.application.port.in.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import java.util.List;

public interface ListFinancialTransactionsUseCase {
  List<FinancialTransaction> listByAccount(String accountId);
}
