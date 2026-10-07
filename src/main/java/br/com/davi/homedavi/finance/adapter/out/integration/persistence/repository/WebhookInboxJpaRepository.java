package br.com.davi.homedavi.finance.adapter.out.integration.persistence.repository;

import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.WebhookInboxEntity;
import br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity.WebhookInboxStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookInboxJpaRepository extends JpaRepository<WebhookInboxEntity, Long> {
  boolean existsByEventId(UUID eventId);

  Optional<WebhookInboxEntity> findByEventId(UUID eventId);

  @Modifying(clearAutomatically = true)
  @Query(
      "update WebhookInboxEntity e set e.status = :newStatus where e.eventId = :eventId and"
          + " e.status in :currentStatuses")
  int updateStatus(
      @Param("eventId") UUID eventId,
      @Param("currentStatuses") Collection<WebhookInboxStatus> currentStatuses,
      @Param("newStatus") WebhookInboxStatus newStatus);
}
