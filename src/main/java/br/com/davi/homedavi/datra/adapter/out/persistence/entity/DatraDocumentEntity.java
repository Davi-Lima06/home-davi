package br.com.davi.homedavi.datra.adapter.out.persistence.entity;

import br.com.davi.homedavi.datra.domain.DatraDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents", schema = "datra")
public class DatraDocumentEntity {
  @Id private UUID id;

  @Column(nullable = false, columnDefinition = "text")
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "source_url", columnDefinition = "text")
  private String sourceUrl;

  @Column(name = "source_reference", columnDefinition = "text")
  private String sourceReference;

  @Column(columnDefinition = "text")
  private String tags;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "text")
  private DatraDocumentStatus status;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DatraDocumentEntity() {}

  public DatraDocumentEntity(
      UUID id,
      String title,
      String content,
      String sourceUrl,
      String sourceReference,
      String tags,
      DatraDocumentStatus status,
      Instant approvedAt,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.sourceUrl = sourceUrl;
    this.sourceReference = sourceReference;
    this.tags = tags;
    this.status = status;
    this.approvedAt = approvedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() { return id; }
  public String getTitle() { return title; }
  public String getContent() { return content; }
  public String getSourceUrl() { return sourceUrl; }
  public String getSourceReference() { return sourceReference; }
  public String getTags() { return tags; }
  public DatraDocumentStatus getStatus() { return status; }
  public Instant getApprovedAt() { return approvedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
