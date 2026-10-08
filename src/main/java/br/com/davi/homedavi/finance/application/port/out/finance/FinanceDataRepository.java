package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import br.com.davi.homedavi.finance.domain.integration.TransactionChanges;
import java.util.List;
import java.util.UUID;

public interface FinanceDataRepository {
  void saveTransactionChanges(TransactionChanges changes);

  /**
   * Cria/atualiza as contas de um item (sincronização disparada ao receber um webhook) e devolve os
   * ids das contas que foram criadas agora (novas).
   */
  List<UUID> saveAccounts(UUID itemId, List<SyncedAccount> accounts);

  void saveLoans(UUID itemId, List<SyncedLoan> loans);

  void saveBills(List<SyncedBill> bills);
}
