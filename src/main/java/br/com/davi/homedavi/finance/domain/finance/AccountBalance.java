package br.com.davi.homedavi.finance.domain.finance;

import br.com.davi.homedavi.finance.domain.integration.SyncedAccount;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Saldo de uma conta no provedor financeiro (Pluggy). Para contas de crédito, o limite disponível.
 */
public record AccountBalance(
    UUID accountId,
    String name,
    String currencyCode,
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal creditLimit) {
  public AccountBalance {
    Objects.requireNonNull(accountId);
  }

  public static AccountBalance from(SyncedAccount account) {
    return new AccountBalance(
        account.id(),
        account.name(),
        account.currencyCode(),
        account.currentBalance(),
        account.availableBalance(),
        account.creditLimit());
  }
}
