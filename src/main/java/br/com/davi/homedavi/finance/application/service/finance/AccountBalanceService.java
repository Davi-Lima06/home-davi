package br.com.davi.homedavi.finance.application.service.finance;

import br.com.davi.homedavi.finance.application.port.in.finance.GetAccountBalanceUseCase;
import br.com.davi.homedavi.finance.application.port.out.integration.FinancialDataProvider;
import br.com.davi.homedavi.finance.domain.finance.AccountBalance;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountBalanceService implements GetAccountBalanceUseCase {
  private static final Logger log = LoggerFactory.getLogger(AccountBalanceService.class);

  private final FinancialDataProvider dataProvider;

  public AccountBalanceService(FinancialDataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  @Override
  public AccountBalance getAccountBalance(UUID accountId) {
    log.info("Fetching account balance from Pluggy: accountId={}", accountId);
    var balance = AccountBalance.from(dataProvider.fetchAccount(accountId));
    log.info(
        "Account balance fetched: accountId={}, currency={}",
        balance.accountId(),
        balance.currencyCode());
    return balance;
  }
}
