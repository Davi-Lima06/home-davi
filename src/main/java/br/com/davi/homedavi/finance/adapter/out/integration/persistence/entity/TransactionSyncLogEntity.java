package br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_sync_log", schema = "finance")
public class TransactionSyncLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * FK para integration.webhook_inbox.event_id (coluna única, não a PK), por isso mapeada como
   * valor.
   */
  @Column(name = "webhook_event_id")
  private UUID webhookEventId;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, columnDefinition = "text")
  private TransactionSyncStatus status = TransactionSyncStatus.STARTED;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  protected TransactionSyncLogEntity() {}

  public TransactionSyncLogEntity(UUID webhookEventId, UUID itemId) {
    this.webhookEventId = webhookEventId;
    this.itemId = itemId;
  }

  public Long getId() {
    return id;
  }

  public UUID getWebhookEventId() {
    return webhookEventId;
  }

  public UUID getItemId() {
    return itemId;
  }

  public TransactionSyncStatus getStatus() {
    return status;
  }

  public void setStatus(TransactionSyncStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
