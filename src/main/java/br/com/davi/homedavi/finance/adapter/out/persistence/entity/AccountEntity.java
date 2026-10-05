package br.com.davi.homedavi.finance.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "accounts", schema = "finance")
public class AccountEntity {
  @Id private UUID id;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(name = "name", nullable = false, columnDefinition = "text")
  private String name;

  @Column(name = "type", columnDefinition = "text")
  private String type;

  @Column(name = "subtype", columnDefinition = "text")
  private String subtype;

  @Column(name = "number_masked", columnDefinition = "text")
  private String numberMasked;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode = "BRL";

  @Column(name = "current_balance", precision = 19, scale = 4)
  private BigDecimal currentBalance;

  @Column(name = "available_balance", precision = 19, scale = 4)
  private BigDecimal availableBalance;

  @Column(name = "credit_limit", precision = 19, scale = 4)
  private BigDecimal creditLimit;

  @Column(name = "status", columnDefinition = "text")
  private String status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_data", nullable = false)
  private String rawData = "{}";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AccountEntity() {}

  public AccountEntity(UUID id, UUID itemId, String name) {
    this.id = id;
    this.itemId = itemId;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public UUID getItemId() {
    return itemId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  public String getNumberMasked() {
    return numberMasked;
  }

  public void setNumberMasked(String numberMasked) {
    this.numberMasked = numberMasked;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public BigDecimal getCurrentBalance() {
    return currentBalance;
  }

  public void setCurrentBalance(BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
  }

  public BigDecimal getAvailableBalance() {
    return availableBalance;
  }

  public void setAvailableBalance(BigDecimal availableBalance) {
    this.availableBalance = availableBalance;
  }

  public BigDecimal getCreditLimit() {
    return creditLimit;
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
