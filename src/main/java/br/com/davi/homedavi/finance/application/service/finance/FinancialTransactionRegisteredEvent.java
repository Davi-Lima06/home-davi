package br.com.davi.homedavi.finance.application.service.finance;

import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;

public record FinancialTransactionRegisteredEvent(FinancialTransaction transaction) {}
