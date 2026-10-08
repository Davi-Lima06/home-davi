package br.com.davi.homedavi.finance.adapter.out.finance.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bills", schema = "finance")
public class BillEntity {
  @Id private UUID id;

  /** FK para finance.accounts(id); mapeada como valor para não exigir a entidade gerenciada. */
  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "total_amount", precision = 19, scale = 4)
  private BigDecimal totalAmount;

  @Column(name = "minimum_payment", precision = 19, scale = 4)
  private BigDecimal minimumPayment;

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

  protected BillEntity() {}

  public BillEntity(UUID id, UUID accountId) {
    this.id = id;
    this.accountId = accountId;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public void setMinimumPayment(BigDecimal minimumPayment) {
    this.minimumPayment = minimumPayment;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }
}
