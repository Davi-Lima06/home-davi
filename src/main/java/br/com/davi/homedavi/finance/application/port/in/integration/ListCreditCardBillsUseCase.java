package br.com.davi.homedavi.finance.application.port.in.integration;

import br.com.davi.homedavi.finance.domain.integration.SyncedBill;
import java.util.List;
import java.util.UUID;

/** Lista, ao vivo no Pluggy, as faturas de um cartão. */
public interface ListCreditCardBillsUseCase {
  List<SyncedBill> listBills(UUID accountId);
}
