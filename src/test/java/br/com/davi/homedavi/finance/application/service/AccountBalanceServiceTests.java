package br.com.davi.homedavi.finance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.davi.homedavi.finance.application.port.out.FinancialDataProvider;
import br.com.davi.homedavi.finance.domain.SyncedAccount;
import br.com.davi.homedavi.finance.domain.SyncedTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountBalanceServiceTests {
  private static final UUID ACCOUNT = UUID.randomUUID();

  @Test
  void mapsPluggyAccountIntoBalance() {
    FinancialDataProvider provider =
        new FinancialDataProvider() {
          @Override
          public SyncedAccount fetchAccount(UUID accountId) {
            assertEquals(ACCOUNT, accountId);
            return new SyncedAccount(
                accountId,
                "Conta Corrente",
                "BANK",
                "CHECKING_ACCOUNT",
                "****6789",
                "BRL",
                new BigDecimal("1500.75"),
                new BigDecimal("1500.75"),
                null,
                "ACTIVE",
                "{}");
          }

          @Override
          public SyncedTransaction fetchTransaction(UUID transactionId) {
            throw new UnsupportedOperationException();
          }

          @Override
          public List<SyncedTransaction> fetchCreatedTransactions(
              UUID accountId, Instant createdAtFrom) {
            throw new UnsupportedOperationException();
          }
        };

    var balance = new AccountBalanceService(provider).getAccountBalance(ACCOUNT);

    assertEquals(ACCOUNT, balance.accountId());
    assertEquals("Conta Corrente", balance.name());
    assertEquals("BRL", balance.currencyCode());
    assertEquals(new BigDecimal("1500.75"), balance.currentBalance());
    assertEquals(new BigDecimal("1500.75"), balance.availableBalance());
  }
}
