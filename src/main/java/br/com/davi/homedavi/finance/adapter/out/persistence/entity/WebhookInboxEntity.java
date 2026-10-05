package br.com.davi.homedavi.finance.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_inbox", schema = "integration")
public class WebhookInboxEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true, updatable = false)
  private UUID eventId;

  @Column(name = "event_type", nullable = false, columnDefinition = "text")
  private String eventType;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "account_id")
  private UUID accountId;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(name = "client_user_id", columnDefinition = "text")
  private String clientUserId;

  @Column(name = "triggered_by", columnDefinition = "text")
  private String triggeredBy;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "transaction_ids", nullable = false)
  private String transactionIds = "[]";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", nullable = false)
  private String rawPayload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, columnDefinition = "text")
  private WebhookInboxStatus status = WebhookInboxStatus.RECEIVED;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  protected WebhookInboxEntity() {}

  public WebhookInboxEntity(
      UUID eventId,
      String eventType,
      UUID itemId,
      UUID accountId,
      UUID clientId,
      String clientUserId,
      String triggeredBy,
      String transactionIds,
      String rawPayload) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.itemId = itemId;
    this.accountId = accountId;
    this.clientId = clientId;
    this.clientUserId = clientUserId;
    this.triggeredBy = triggeredBy;
    if (transactionIds != null) this.transactionIds = transactionIds;
    this.rawPayload = rawPayload;
  }

  public Long getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public UUID getItemId() {
    return itemId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public String getClientUserId() {
    return clientUserId;
  }

  public String getTriggeredBy() {
    return triggeredBy;
  }

  public String getTransactionIds() {
    return transactionIds;
  }

  public String getRawPayload() {
    return rawPayload;
  }

  public WebhookInboxStatus getStatus() {
    return status;
  }

  public void setStatus(WebhookInboxStatus status) {
    this.status = status;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}
