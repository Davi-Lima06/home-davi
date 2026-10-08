package br.com.davi.homedavi.finance.adapter.out.integration.persistence;

import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.WebhookInboxEntity;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.WebhookInboxStatus;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.repository.WebhookInboxJpaRepository;
import br.com.davi.homedavi.finance.application.port.out.integration.WebhookEventQueue;
import br.com.davi.homedavi.finance.domain.integration.PluggyWebhookEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaWebhookEventQueue implements WebhookEventQueue {
    private static final Logger log = LoggerFactory.getLogger(JpaWebhookEventQueue.class);

    private static final List<WebhookInboxStatus> PENDING =
            List.of(WebhookInboxStatus.RECEIVED, WebhookInboxStatus.FAILED);

    private final WebhookInboxJpaRepository inboxRepository;

    public JpaWebhookEventQueue(WebhookInboxJpaRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    private static PluggyWebhookEvent toDomain(WebhookInboxEntity entity) {
        return new PluggyWebhookEvent(
                entity.getEventId(),
                entity.getEventType(),
                entity.getItemId(),
                entity.getAccountId(),
                entity.getClientId(),
                entity.getClientUserId(),
                entity.getTriggeredBy(),
                entity.getTransactionIds(),
                entity.getRawPayload());
    }

    @Override
    @Transactional
    public Optional<PluggyWebhookEvent> claimPendingEvent(UUID eventId) {
        if (inboxRepository.updateStatus(eventId, PENDING, WebhookInboxStatus.PROCESSING) == 0)
            return Optional.empty();
        return inboxRepository.findByEventId(eventId).map(JpaWebhookEventQueue::toDomain);
    }

    @Override
    @Transactional
    public void markProcessed(UUID eventId) {
        var entity = find(eventId);
        entity.setStatus(WebhookInboxStatus.PROCESSED);
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setProcessedAt(Instant.now());
        entity.setLastError(null);
    }

    @Override
    @Transactional
    public void markFailed(UUID eventId, String error) {
        var entity = find(eventId);
        entity.setStatus(WebhookInboxStatus.FAILED);
        entity.setAttempts(entity.getAttempts() + 1);
        entity.setLastError(error);
        log.warn(
                "[6/7] Pluggy webhook marked as failed: eventId={}, attempts={}",
                eventId,
                entity.getAttempts());
    }

    private WebhookInboxEntity find(UUID eventId) {
        return inboxRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Webhook event not found: " + eventId));
    }
}
