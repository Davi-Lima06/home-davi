package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.FinancialAccount;
import java.util.List;

public interface FinancialAccountRepository {
  List<FinancialAccount> findAll();
}
