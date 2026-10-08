package br.com.davi.homedavi.finance.application.port.in.finance;

import br.com.davi.homedavi.finance.domain.finance.TransactionsByType;
import java.time.LocalDate;

public interface ListFinancialTransactionsUseCase {
  /**
   * Transações por tipo de conta e período. type: "credit" (cartão), "bank" (pix) ou nulo (ambos,
   * separados). from/to nulos assumem os últimos 30 dias.
   */
  TransactionsByType getTransactions(String type, LocalDate from, LocalDate to);
}
