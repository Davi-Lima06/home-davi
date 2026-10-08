package br.com.davi.homedavi.finance.application.service.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.in.finance.RegisterFinancialTransactionUseCase.RegisterFinancialTransactionCommand;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FinancialTransactionServiceTests {
  private static final String ACCOUNT = UUID.randomUUID().toString();

  private final RecordingRepository repository = new RecordingRepository();
  private final List<Object> published = new ArrayList<>();
  private final ApplicationEventPublisher events = published::add;
  private final FinancialTransactionService service =
      new FinancialTransactionService(repository, events);

  @Test
  void returnsBothTypesSeparatedAndDefaultsToLast30DaysWhenNull() {
    var result = service.getTransactions(null, null, null);

    assertEquals(List.of("CREDIT", "BANK"), repository.queriedTypes);
    assertEquals(1, result.credit().size());
    assertEquals(1, result.bank().size());
    // janela padrão: [hoje-30, hoje] → intervalo exclusivo de 31 dias
    assertEquals(31, ChronoUnit.DAYS.between(repository.lastFrom, repository.lastToExclusive));
  }

  @Test
  void queriesOnlyCreditWhenTypeIsCredit() {
    var result = service.getTransactions("credit", null, null);

    assertEquals(List.of("CREDIT"), repository.queriedTypes);
    assertEquals(1, result.credit().size());
    assertTrue(result.bank().isEmpty());
  }

  @Test
  void rejectsUnknownType() {
    assertThrows(IllegalArgumentException.class, () -> service.getTransactions("foo", null, null));
  }

  @Test
  void registerPersistsAndPublishesEvent() {
    var saved =
        service.register(
            new RegisterFinancialTransactionCommand(
                ACCOUNT, "Mercado", new BigDecimal("12.30"), "BRL", TransactionType.EXPENSE, null, null));

    assertSame(saved, repository.lastSaved);
    assertEquals(ACCOUNT, saved.accountId());
    assertTrue(published.stream().anyMatch(e -> e instanceof FinancialTransactionRegisteredEvent));
  }

  private static final class RecordingRepository implements FinancialTransactionRepository {
    private FinancialTransaction lastSaved;
    private final List<String> queriedTypes = new ArrayList<>();
    private Instant lastFrom;
    private Instant lastToExclusive;

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
      this.lastSaved = transaction;
      return transaction;
    }

    @Override
    public List<FinancialTransaction> findByAccountType(
        String accountType, Instant fromInclusive, Instant toExclusive) {
      queriedTypes.add(accountType);
      lastFrom = fromInclusive;
      lastToExclusive = toExclusive;
      return List.of(
          new FinancialTransaction(
              UUID.randomUUID(), ACCOUNT, accountType + " tx", new BigDecimal("12.30"), "BRL",
              TransactionType.EXPENSE, Instant.now(), null));
    }
  }
}
