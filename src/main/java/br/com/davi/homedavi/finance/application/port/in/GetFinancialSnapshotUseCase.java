package br.com.davi.homedavi.finance.application.port.in;

import br.com.davi.homedavi.finance.domain.FinancialSnapshot;

public interface GetFinancialSnapshotUseCase {
  FinancialSnapshot getFinancialSnapshot();
}
