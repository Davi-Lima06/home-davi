package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public interface RegisterFinancialTransactionUseCase {
  FinancialTransaction register(RegisterFinancialTransactionCommand command);

  record RegisterFinancialTransactionCommand(
      String accountId,
      String description,
      BigDecimal amount,
      String currency,
      TransactionType type,
      Instant occurredAt,
      String externalId) {}
}
