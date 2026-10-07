package br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions", schema = "finance")
public class TransactionEntity {
  @Id private UUID id;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private AccountEntity account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private CategoryEntity category;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description;

  @Column(name = "merchant_name", columnDefinition = "text")
  private String merchantName;

  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode = "BRL";

  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false, columnDefinition = "text")
  private FinanceTransactionType transactionType;

  @Column(name = "status", columnDefinition = "text")
  private String status;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "is_pending", nullable = false)
  private boolean pending;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_data", nullable = false)
  private String rawData = "{}";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TransactionEntity() {}

  public TransactionEntity(
      UUID id,
      UUID itemId,
      AccountEntity account,
      String description,
      BigDecimal amount,
      FinanceTransactionType transactionType,
      Instant occurredAt) {
    this.id = id;
    this.itemId = itemId;
    this.account = account;
    this.description = description;
    this.amount = amount;
    this.transactionType = transactionType;
    this.occurredAt = occurredAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getItemId() {
    return itemId;
  }

  public AccountEntity getAccount() {
    return account;
  }

  public CategoryEntity getCategory() {
    return category;
  }

  public void setCategory(CategoryEntity category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMerchantName() {
    return merchantName;
  }

  public void setMerchantName(String merchantName) {
    this.merchantName = merchantName;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public FinanceTransactionType getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(FinanceTransactionType transactionType) {
    this.transactionType = transactionType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public void setPostedAt(Instant postedAt) {
    this.postedAt = postedAt;
  }

  public boolean isPending() {
    return pending;
  }

  public void setPending(boolean pending) {
    this.pending = pending;
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
