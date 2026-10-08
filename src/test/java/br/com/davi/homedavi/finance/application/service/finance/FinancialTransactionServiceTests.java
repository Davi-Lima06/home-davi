package br.com.davi.homedavi.finance.application.service.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.davi.homedavi.finance.application.port.in.finance.RegisterFinancialTransactionUseCase.RegisterFinancialTransactionCommand;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.finance.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
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
  void listByAccountReadsFromTheRepository() {
    var result = service.listByAccount(ACCOUNT);

    assertEquals(1, result.size());
    assertEquals(ACCOUNT, result.getFirst().accountId());
  }

  @Test
  void registerPersistsAndPublishesEvent() {
    var saved =
        service.register(
            new RegisterFinancialTransactionCommand(
                ACCOUNT, "Mercado", new BigDecimal("12.30"), "BRL", TransactionType.EXPENSE, null, null));

    assertSame(saved, repository.lastSaved, "deveria retornar o que o repositório salvou");
    assertEquals(ACCOUNT, saved.accountId());
    assertTrue(published.stream().anyMatch(e -> e instanceof FinancialTransactionRegisteredEvent));
  }

  private static final class RecordingRepository implements FinancialTransactionRepository {
    private FinancialTransaction lastSaved;

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
      this.lastSaved = transaction;
      return transaction;
    }

    @Override
    public List<FinancialTransaction> findAll() {
      return List.of(
          new FinancialTransaction(
              UUID.randomUUID(), ACCOUNT, "Mercado", new BigDecimal("12.30"), "BRL",
              TransactionType.EXPENSE, Instant.now(), null));
    }
  }
}
