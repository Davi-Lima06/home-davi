package br.com.davi.homedavi.finance.application.port.in.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialSnapshot;

public interface GetFinancialSnapshotUseCase {
  FinancialSnapshot getFinancialSnapshot();
}
