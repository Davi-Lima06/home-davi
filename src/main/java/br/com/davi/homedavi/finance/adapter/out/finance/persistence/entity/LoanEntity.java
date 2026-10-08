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
@Table(name = "loans", schema = "finance")
public class LoanEntity {
  @Id private UUID id;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(name = "contract_number", columnDefinition = "text")
  private String contractNumber;

  @Column(name = "outstanding_balance", precision = 19, scale = 4)
  private BigDecimal outstandingBalance;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode = "BRL";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_data", nullable = false)
  private String rawData = "{}";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected LoanEntity() {}

  public LoanEntity(UUID id, UUID itemId) {
    this.id = id;
    this.itemId = itemId;
  }

  public UUID getId() {
    return id;
  }

  public UUID getItemId() {
    return itemId;
  }

  public void setContractNumber(String contractNumber) {
    this.contractNumber = contractNumber;
  }

  public void setOutstandingBalance(BigDecimal outstandingBalance) {
    this.outstandingBalance = outstandingBalance;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }
}
