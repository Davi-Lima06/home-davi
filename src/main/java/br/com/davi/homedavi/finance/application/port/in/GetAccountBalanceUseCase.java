package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.AccountBalance;
import java.util.UUID;

public interface GetAccountBalanceUseCase {
  AccountBalance getAccountBalance(UUID accountId);
}
