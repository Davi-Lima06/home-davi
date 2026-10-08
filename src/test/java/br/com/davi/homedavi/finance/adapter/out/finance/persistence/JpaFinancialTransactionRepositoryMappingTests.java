package br.com.davi.homedavi.finance.adapter.out.finance.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.AccountEntity;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.FinanceTransactionType;
import br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity.TransactionEntity;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaFinancialTransactionRepositoryMappingTests {

  @Test
  void mapsEntityToDomainIncludingAccountIdAndType() {
    UUID accountId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-10-01T12:00:00Z");
    var account = new AccountEntity(accountId, itemId, "Conta Corrente");
    var entity =
        new TransactionEntity(
            txId, itemId, account, "Mercado", new BigDecimal("12.30"),
            FinanceTransactionType.EXPENSE, occurredAt);
    entity.setCurrencyCode("BRL");

    var domain = JpaFinancialTransactionRepository.toDomain(entity);

    assertEquals(txId, domain.id());
    assertEquals(accountId.toString(), domain.accountId());
    assertEquals("Mercado", domain.description());
    assertEquals(new BigDecimal("12.30"), domain.amount());
    assertEquals("BRL", domain.currency());
    assertEquals(TransactionType.EXPENSE, domain.type());
    assertEquals(occurredAt, domain.occurredAt());
    assertEquals(txId.toString(), domain.externalId());
  }

  @Test
  void mapsTransferTypeThatTheOldDomainCouldNotRepresent() {
    var account = new AccountEntity(UUID.randomUUID(), UUID.randomUUID(), "Conta");
    var entity =
        new TransactionEntity(
            UUID.randomUUID(), UUID.randomUUID(), account, "Transferência",
            new BigDecimal("500.00"), FinanceTransactionType.TRANSFER, Instant.now());

    var domain = JpaFinancialTransactionRepository.toDomain(entity);

    assertEquals(TransactionType.TRANSFER, domain.type());
  }
}
