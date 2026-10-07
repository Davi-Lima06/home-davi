package br.com.davi.homedavi.finance.application.port.in.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialAccount;
import java.util.List;

public interface ListFinancialAccountsUseCase {
  List<FinancialAccount> listAccounts();
}
