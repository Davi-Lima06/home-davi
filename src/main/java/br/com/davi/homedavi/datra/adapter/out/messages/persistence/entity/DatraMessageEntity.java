package br.com.davi.homedavi.datra.adapter.out.messages.persistence.entity;

import br.com.davi.homedavi.datra.domain.messages.MessageDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "message_inbox", schema = "datra")
public class DatraMessageEntity {
  @Id
  @Column(name = "source_message_id")
  private long sourceMessageId;

  @Column(nullable = false, length = 15)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "text")
  private MessageDirection direction;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "source_event_type", nullable = false, length = 10)
  private String sourceEventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", nullable = false)
  private String rawPayload;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DatraMessageEntity() {}

  public DatraMessageEntity(
      long sourceMessageId,
      String phone,
      MessageDirection direction,
      String content,
      Instant occurredAt,
      String sourceEventType,
      String rawPayload,
      Instant receivedAt,
      Instant updatedAt) {
    this.sourceMessageId = sourceMessageId;
    this.phone = phone;
    this.direction = direction;
    this.content = content;
    this.occurredAt = occurredAt;
    this.sourceEventType = sourceEventType;
    this.rawPayload = rawPayload;
    this.receivedAt = receivedAt;
    this.updatedAt = updatedAt;
  }

  public long getSourceMessageId() { return sourceMessageId; }
  public String getPhone() { return phone; }
  public MessageDirection getDirection() { return direction; }
  public String getContent() { return content; }
  public Instant getOccurredAt() { return occurredAt; }
  public String getSourceEventType() { return sourceEventType; }
  public String getRawPayload() { return rawPayload; }
  public Instant getReceivedAt() { return receivedAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
