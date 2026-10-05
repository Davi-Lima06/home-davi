package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.FinancialAccount;
import java.util.List;

public interface ListFinancialAccountsUseCase {
  List<FinancialAccount> listAccounts();
}
