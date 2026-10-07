package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedInvestment;
import java.util.List;
import java.util.UUID;

/** Lista, ao vivo no Pluggy, os investimentos de uma conexão (item). */
public interface ListInvestmentsUseCase {
  List<SyncedInvestment> listInvestments(UUID itemId);
}
