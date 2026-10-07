package br.com.davi.homedavi.finance.application.port.in.finance;

import br.com.davi.homedavi.finance.domain.finance.AccountBalance;
import java.util.UUID;

public interface GetAccountBalanceUseCase {
  AccountBalance getAccountBalance(UUID accountId);
}
