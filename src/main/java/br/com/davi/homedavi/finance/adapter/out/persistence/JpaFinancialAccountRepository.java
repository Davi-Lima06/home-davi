package br.com.davi.homedavi.finance.adapter.out.persistence;

import br.com.davi.homedavi.finance.adapter.out.persistence.repository.AccountJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.FinancialAccountRepository;
import br.com.davi.homedavi.finance.domain.FinancialAccount;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFinancialAccountRepository implements FinancialAccountRepository {
  private final AccountJpaRepository repository;

  public JpaFinancialAccountRepository(AccountJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<FinancialAccount> findAll() {
    return repository.findAllByOrderByNameAsc().stream()
        .map(
            account ->
                new FinancialAccount(
                    account.getId(),
                    account.getName(),
                    account.getType(),
                    account.getSubtype(),
                    account.getCurrencyCode()))
        .toList();
  }
}
