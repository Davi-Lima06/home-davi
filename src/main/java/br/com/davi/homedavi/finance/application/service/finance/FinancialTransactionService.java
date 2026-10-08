package br.com.davi.homedavi.finance.application.service.finance;

import br.com.davi.homedavi.finance.application.port.in.finance.ListFinancialTransactionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.finance.RegisterFinancialTransactionUseCase;
import br.com.davi.homedavi.finance.application.port.out.finance.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.finance.FinancialTransaction;
import br.com.davi.homedavi.finance.domain.finance.TransactionsByType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class FinancialTransactionService
    implements RegisterFinancialTransactionUseCase, ListFinancialTransactionsUseCase {
  private final FinancialTransactionRepository repository;
  private final ApplicationEventPublisher events;

  public FinancialTransactionService(
      FinancialTransactionRepository repository, ApplicationEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  @Override
  public FinancialTransaction register(RegisterFinancialTransactionCommand command) {
    var transaction =
        new FinancialTransaction(
            UUID.randomUUID(),
            command.accountId(),
            command.description(),
            command.amount(),
            command.currency(),
            command.type(),
            command.occurredAt() == null ? Instant.now() : command.occurredAt(),
            command.externalId());
    var saved = repository.save(transaction);
    events.publishEvent(new FinancialTransactionRegisteredEvent(saved));
    return saved;
  }

  private static final ZoneOffset ZONE = ZoneOffset.UTC;
  private static final int DEFAULT_WINDOW_DAYS = 30;

  @Override
  public TransactionsByType getTransactions(String type, LocalDate from, LocalDate to) {
    String normalized = type == null || type.isBlank() ? null : type.trim().toLowerCase();
    if (normalized != null && !normalized.equals("credit") && !normalized.equals("bank")) {
      throw new IllegalArgumentException("type deve ser 'credit', 'bank' ou nulo: " + type);
    }
    LocalDate toDate = to == null ? LocalDate.now(ZONE) : to;
    LocalDate fromDate = from == null ? toDate.minusDays(DEFAULT_WINDOW_DAYS) : from;
    Instant fromInstant = fromDate.atStartOfDay(ZONE).toInstant();
    Instant toExclusive = toDate.plusDays(1).atStartOfDay(ZONE).toInstant();

    List<FinancialTransaction> credit =
        normalized == null || normalized.equals("credit")
            ? repository.findByAccountType("CREDIT", fromInstant, toExclusive)
            : List.of();
    List<FinancialTransaction> bank =
        normalized == null || normalized.equals("bank")
            ? repository.findByAccountType("BANK", fromInstant, toExclusive)
            : List.of();
    return new TransactionsByType(credit, bank);
  }
}
