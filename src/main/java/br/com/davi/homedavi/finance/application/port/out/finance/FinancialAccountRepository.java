package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialAccount;
import java.util.List;

public interface FinancialAccountRepository {
  List<FinancialAccount> findAll();
}
