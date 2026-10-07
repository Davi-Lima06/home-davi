package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedItem;
import java.util.List;

/** Lista, ao vivo no Pluggy (API v2), as conexões bancárias (items) e seus estados. */
public interface ListBankConnectionsUseCase {
  List<SyncedItem> listItems();
}
