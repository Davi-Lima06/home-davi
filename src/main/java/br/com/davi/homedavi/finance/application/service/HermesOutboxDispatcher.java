package br.com.davi.homedavi.finance.application.service;

import br.com.davi.homedavi.finance.application.port.out.HermesNotificationOutbox;
import br.com.davi.homedavi.finance.application.port.out.HermesNotificationPublisher;
import br.com.davi.homedavi.finance.domain.HermesOutboxMessage;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Entrega assíncrona, com lease e backoff, das notificações já persistidas. */
@Component
@ConditionalOnProperty(prefix = "hermes.webhook", name = "enabled", havingValue = "true")
public class HermesOutboxDispatcher {
  private static final Logger log = LoggerFactory.getLogger(HermesOutboxDispatcher.class);
  private final HermesNotificationOutbox outbox;
  private final HermesNotificationPublisher publisher;
  private final int batchSize;

  public HermesOutboxDispatcher(
      HermesNotificationOutbox outbox,
      HermesNotificationPublisher publisher,
      @Value("${hermes.webhook.batch-size:20}") int batchSize) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.batchSize = Math.max(1, batchSize);
  }

  @Scheduled(fixedDelayString = "${hermes.webhook.poll-delay:PT5S}")
  public void dispatch() {
    Instant now = Instant.now();
    for (HermesOutboxMessage message : outbox.claimReady(batchSize, now)) {
      try {
        publisher.publish(message);
        outbox.markSent(message.id(), Instant.now());
        log.info(
            "[7/7] Financial summary delivered to Hermes: eventId={}, outboxId={}",
            message.webhookEventId(),
            message.id());
      } catch (RuntimeException exception) {
        Duration delay = retryDelay(message.attempts());
        String error =
            exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        outbox.markForRetry(message.id(), Instant.now().plus(delay), abbreviate(error));
        log.warn(
            "Could not deliver financial summary to Hermes; retry in {}: eventId={}, outboxId={}",
            delay,
            message.webhookEventId(),
            message.id());
      }
    }
  }

  private static Duration retryDelay(int attempts) {
    long seconds = Math.min(3600, 5L * (1L << Math.min(Math.max(0, attempts - 1), 9)));
    return Duration.ofSeconds(seconds);
  }

  private static String abbreviate(String error) {
    return error.length() <= 1_000 ? error : error.substring(0, 1_000);
  }
}
