package br.com.davi.homedavi.finance.adapter.out.persistence;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.HermesOutboxEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.entity.HermesOutboxStatus;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.HermesOutboxJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaHermesNotificationOutbox implements HermesNotificationOutbox {
  private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

  private final HermesOutboxJpaRepository repository;

  public JpaHermesNotificationOutbox(HermesOutboxJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void enqueue(HermesOutboxMessage message) {
    repository.save(
        new HermesOutboxEntity(
            message.webhookEventId(), message.pluggyEventType(), message.summaryJson()));
  }

  @Override
  @Transactional
  public List<HermesOutboxMessage> claimReady(int limit, Instant now) {
    return repository.findReadyForDispatch(now, PageRequest.of(0, limit)).stream()
        .map(
            entity -> {
              entity.setStatus(HermesOutboxStatus.SENDING);
              entity.setAttempts(entity.getAttempts() + 1);
              // A mesma mensagem não pode ser reenviada por outro nó enquanto a chamada HTTP
              // estiver em curso.
              entity.setAvailableAt(now.plus(LEASE_DURATION));
              return toMessage(entity);
            })
        .toList();
  }

  @Override
  @Transactional
  public void markSent(long outboxId, Instant sentAt) {
    repository
        .findById(outboxId)
        .ifPresent(
            entity -> {
              entity.setStatus(HermesOutboxStatus.SENT);
              entity.setSentAt(sentAt);
              entity.setLastError(null);
            });
  }

  @Override
  @Transactional
  public void markForRetry(long outboxId, Instant availableAt, String error) {
    repository
        .findById(outboxId)
        .ifPresent(
            entity -> {
              entity.setStatus(HermesOutboxStatus.FAILED);
              entity.setAvailableAt(availableAt);
              entity.setLastError(error);
            });
  }

  private static HermesOutboxMessage toMessage(HermesOutboxEntity entity) {
    return new HermesOutboxMessage(
        entity.getId(),
        entity.getWebhookEventId(),
        entity.getEventType(),
        entity.getPayload(),
        entity.getAttempts());
  }
}
