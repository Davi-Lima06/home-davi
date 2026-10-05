package br.com.davi.homedavi.finance.adapter.out.persistence;

import br.com.davi.homedavi.finance.adapter.out.persistence.entity.WebhookInboxEntity;
import br.com.davi.homedavi.finance.adapter.out.persistence.repository.WebhookInboxJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.PluggyWebhookInbox;
import br.com.davi.homedavi.finance.domain.PluggyWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JpaPluggyWebhookInbox implements PluggyWebhookInbox {
  private static final Logger log = LoggerFactory.getLogger(JpaPluggyWebhookInbox.class);

  private final WebhookInboxJpaRepository inboxRepository;
  private final TransactionTemplate transactionTemplate;

  public JpaPluggyWebhookInbox(
      WebhookInboxJpaRepository inboxRepository, TransactionTemplate transactionTemplate) {
    this.inboxRepository = inboxRepository;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public boolean persist(PluggyWebhookEvent event) {
    if (inboxRepository.existsByEventId(event.eventId())) return false;
    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            inboxRepository.saveAndFlush(
                new WebhookInboxEntity(
                    event.eventId(),
                    event.eventType(),
                    event.itemId(),
                    event.accountId(),
                    event.clientId(),
                    event.clientUserId(),
                    event.triggeredBy(),
                    event.transactionIdsJson(),
                    event.rawPayloadJson()));
          });
      log.debug("Inbox row inserted: eventId={}", event.eventId());
      return true;
    } catch (DataIntegrityViolationException exception) {
      // Entrega concorrente do mesmo evento: a UNIQUE(event_id) garante a idempotência, como o ON
      // CONFLICT fazia.
      if (inboxRepository.existsByEventId(event.eventId())) {
        log.debug(
            "Concurrent delivery of the same webhook resolved by unique constraint: eventId={}",
            event.eventId());
        return false;
      }
      log.error("Could not store Pluggy webhook: eventId={}", event.eventId(), exception);
      throw exception;
    }
  }
}
