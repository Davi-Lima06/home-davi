package br.com.davi.homedavi.finance.application.port.out;

import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;
import java.time.Instant;
import java.util.List;

/**
 * Outbox transacional: o Hermes só é chamado depois de a alteração financeira ter sido concluída.
 */
public interface HermesNotificationOutbox {
  void enqueue(HermesOutboxMessage message);

  List<HermesOutboxMessage> claimReady(int limit, Instant now);

  void markSent(long outboxId, Instant sentAt);

  void markForRetry(long outboxId, Instant availableAt, String error);
}
