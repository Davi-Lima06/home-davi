package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.TransactionPage;
import java.util.UUID;

/** Lista, ao vivo no Pluggy (API v2), uma página de transações de uma conta. */
public interface ListAccountTransactionsUseCase {
  TransactionPage listTransactions(UUID accountId, String cursor, Integer pageSize);
}
