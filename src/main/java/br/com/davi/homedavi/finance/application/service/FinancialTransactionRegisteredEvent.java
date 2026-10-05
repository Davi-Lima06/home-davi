package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.domain.FinancialTransaction;

public record FinancialTransactionRegisteredEvent(FinancialTransaction transaction) {}
