package br.com.davi.homedavi.finance.adapter.out.integration.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hermes_outbox", schema = "integration")
public class HermesOutboxEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** FK para webhook_inbox.event_id (coluna única, não a PK), por isso mapeada como valor. */
  @Column(name = "webhook_event_id", nullable = false, updatable = false)
  private UUID webhookEventId;

  @Column(name = "event_type", nullable = false, columnDefinition = "text")
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, columnDefinition = "text")
  private HermesOutboxStatus status = HermesOutboxStatus.PENDING;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt = Instant.now();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  protected HermesOutboxEntity() {}

  public HermesOutboxEntity(UUID webhookEventId, String eventType, String payload) {
    this.webhookEventId = webhookEventId;
    this.eventType = eventType;
    this.payload = payload;
  }

  public Long getId() {
    return id;
  }

  public UUID getWebhookEventId() {
    return webhookEventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public HermesOutboxStatus getStatus() {
    return status;
  }

  public void setStatus(HermesOutboxStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getAvailableAt() {
    return availableAt;
  }

  public void setAvailableAt(Instant availableAt) {
    this.availableAt = availableAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}
