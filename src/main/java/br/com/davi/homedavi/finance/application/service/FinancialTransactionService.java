package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.application.port.in.ListFinancialTransactionsUseCase;
import br.com.davi.homedavi.finance.application.port.in.RegisterFinancialTransactionUseCase;
import br.com.davi.homedavi.finance.application.port.out.FinancialTransactionRepository;
import br.com.davi.homedavi.finance.domain.FinancialTransaction;
import java.time.Instant;
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

  @Override
  public List<FinancialTransaction> listByAccount(String accountId) {
    return repository.findByAccountId(accountId);
  }
}
