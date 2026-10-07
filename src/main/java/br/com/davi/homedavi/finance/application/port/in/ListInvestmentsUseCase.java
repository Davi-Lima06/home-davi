package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.SyncedInvestment;
import java.util.List;
import java.util.UUID;

/** Lista, ao vivo no Pluggy, os investimentos de uma conexão (item). */
public interface ListInvestmentsUseCase {
  List<SyncedInvestment> listInvestments(UUID itemId);
}
