package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedLoan;
import java.util.List;
import java.util.UUID;

/** Lista, ao vivo no Pluggy, os empréstimos de uma conexão (item). */
public interface ListLoansUseCase {
  List<SyncedLoan> listLoans(UUID itemId);
}
