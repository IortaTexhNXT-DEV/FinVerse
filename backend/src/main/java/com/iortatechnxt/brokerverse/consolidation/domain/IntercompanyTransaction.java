package com.iortatechnxt.brokerverse.consolidation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inter-company transaction: one business event recorded by two mirror journals (one per company)
 * sharing the same {@code icReference}, value date and amount.
 */
@Entity
@Table(name = "ic_transaction")
public class IntercompanyTransaction extends BaseEntity {

  @Column(name = "ic_reference", nullable = false, length = 40, unique = true)
  private String icReference;

  @Column(name = "relationship_id", nullable = false)
  private Long relationshipId;

  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false, length = 12)
  private IntercompanyTransactionType type;

  @Column(name = "creditor_company_id", nullable = false)
  private Long creditorCompanyId;

  @Column(name = "debtor_company_id", nullable = false)
  private Long debtorCompanyId;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "creditor_account", nullable = false, length = 30)
  private String creditorAccount;

  @Column(name = "debtor_account", nullable = false, length = 30)
  private String debtorAccount;

  @Column(nullable = false, length = 250)
  private String narration;

  @Column(name = "creditor_batch_no", nullable = false, length = 40)
  private String creditorBatchNo;

  @Column(name = "debtor_batch_no", nullable = false, length = 40)
  private String debtorBatchNo;

  protected IntercompanyTransaction() {}

  /**
   * Records a posted inter-company transaction.
   *
   * @param icReference shared reference
   * @param relationshipId relationship
   * @param values business values
   * @param creditorBatchNo journal posted in the creditor company
   * @param debtorBatchNo journal posted in the debtor company
   */
  public IntercompanyTransaction(
      String icReference,
      Long relationshipId,
      IntercompanyValues values,
      String creditorBatchNo,
      String debtorBatchNo) {
    this.icReference = icReference;
    this.relationshipId = relationshipId;
    this.type = values.type();
    this.creditorCompanyId = values.creditorCompanyId();
    this.debtorCompanyId = values.debtorCompanyId();
    this.valueDate = values.valueDate();
    this.currency = values.currency();
    this.amount = values.amount();
    this.creditorAccount = values.creditorAccount();
    this.debtorAccount = values.debtorAccount();
    this.narration = values.narration();
    this.creditorBatchNo = creditorBatchNo;
    this.debtorBatchNo = debtorBatchNo;
  }

  public String getIcReference() {
    return icReference;
  }

  public Long getRelationshipId() {
    return relationshipId;
  }

  public IntercompanyTransactionType getType() {
    return type;
  }

  public Long getCreditorCompanyId() {
    return creditorCompanyId;
  }

  public Long getDebtorCompanyId() {
    return debtorCompanyId;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCreditorAccount() {
    return creditorAccount;
  }

  public String getDebtorAccount() {
    return debtorAccount;
  }

  public String getNarration() {
    return narration;
  }

  public String getCreditorBatchNo() {
    return creditorBatchNo;
  }

  public String getDebtorBatchNo() {
    return debtorBatchNo;
  }
}
