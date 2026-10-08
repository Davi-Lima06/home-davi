package br.com.davi.homedavi.finance.application.port.out.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import java.time.Instant;
import java.util.List;

public interface FinancialTransactionRepository {
  FinancialTransaction save(FinancialTransaction transaction);

  /** Transações cujas contas têm o tipo informado (ex.: CREDIT, BANK), no intervalo [from, toExclusive). */
  List<FinancialTransaction> findByAccountType(String accountType, Instant fromInclusive, Instant toExclusive);
}
