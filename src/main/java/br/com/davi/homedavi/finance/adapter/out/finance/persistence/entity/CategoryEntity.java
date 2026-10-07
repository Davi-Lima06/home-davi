package br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "categories",
    schema = "finance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "kind"}))
public class CategoryEntity {
  /** Gerado pela aplicação; o banco não usa pgcrypto. */
  @Id private UUID id;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, columnDefinition = "text")
  private CategoryKind kind;

  @Column(name = "color", columnDefinition = "text")
  private String color;

  @Column(name = "icon", columnDefinition = "text")
  private String icon;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected CategoryEntity() {}

  public CategoryEntity(String name, CategoryKind kind) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.kind = kind;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CategoryKind getKind() {
    return kind;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
