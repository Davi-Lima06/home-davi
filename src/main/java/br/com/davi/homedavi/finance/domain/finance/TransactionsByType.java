package br.com.davi.homedavi.finance.domain.finance;

import java.util.List;

/**
 * Transações separadas por tipo de conta. credit = cartão, bank = conta/pix. Quando a consulta
 * filtra um tipo específico, a outra lista vem vazia.
 */
public record TransactionsByType(List<FinancialTransaction> credit, List<FinancialTransaction> bank) {
  public TransactionsByType {
    credit = List.copyOf(credit);
    bank = List.copyOf(bank);
  }
}
