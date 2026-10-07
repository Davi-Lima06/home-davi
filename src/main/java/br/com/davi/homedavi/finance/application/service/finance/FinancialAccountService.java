package br.com.davi.homedavi.finance.application.service.finance;

import br.com.davi.homedavi.finance.application.port.in.finance.ListFinancialAccountsUseCase;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialAccountRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialAccount;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinancialAccountService implements ListFinancialAccountsUseCase {
  private final FinancialAccountRepository repository;

  public FinancialAccountService(FinancialAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<FinancialAccount> listAccounts() {
    return repository.findAll();
  }
}
