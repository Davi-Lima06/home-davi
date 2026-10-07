package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import java.util.List;
import java.util.UUID;

/** Lista, ao vivo no Pluggy, as contas e cartões de uma conexão (item). */
public interface ListConnectedAccountsUseCase {
  List<SyncedAccount> listAccounts(UUID itemId);
}
