package com.iortatechnxt.finverse.subledger.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Open item of a party sub-ledger (receivables, payables, reinsurance and coinsurance balances).
 *
 * <p>Each business document that creates a party balance (debit note, supplier invoice, receipt,
 * claim payable, reinsurance statement) records one open item alongside its GL posting. Matching
 * DEBIT against CREDIT items gives statements of account, outstanding balances and ageing.
 */
@Entity
@Table(name = "sl_open_item")
public class OpenItem extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private ItemDirection direction;

  @Column(name = "document_type", nullable = false, length = 30)
  private String documentType;

  @Column(name = "document_no", nullable = false, length = 40)
  private String documentNo;

  @Column(name = "document_date", nullable = false)
  private LocalDate documentDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "settled_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal settledAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OpenItemStatus status = OpenItemStatus.OPEN;

  @Column(name = "source_module", nullable = false, length = 30)
  private String sourceModule;

  @Column(name = "source_reference", length = 80)
  private String sourceReference;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(length = 250)
  private String narration;

  protected OpenItem() {}

  /**
   * Creates an open item.
   *
   * @param v values
   */
  public OpenItem(OpenItemValues v) {
    this.companyId = v.companyId();
    this.branchId = v.branchId();
    this.partyId = v.partyId();
    this.partyCode = v.partyCode();
    this.direction = v.direction();
    this.documentType = v.documentType();
    this.documentNo = v.documentNo();
    this.documentDate = v.documentDate();
    this.dueDate = v.dueDate();
    this.currency = v.currency();
    this.amount = v.amount();
    this.baseAmount = v.baseAmount();
    this.sourceModule = v.sourceModule();
    this.sourceReference = v.sourceReference();
    this.journalBatchNo = v.journalBatchNo();
    this.narration = v.narration();
  }

  /**
   * Outstanding (unsettled) amount in item currency.
   *
   * @return amount minus settled amount
   */
  public BigDecimal outstanding() {
    return status == OpenItemStatus.WRITTEN_OFF ? BigDecimal.ZERO : amount.subtract(settledAmount);
  }

  /**
   * Applies a settlement.
   *
   * @param value amount settled (positive, not exceeding outstanding)
   */
  public void settle(BigDecimal value) {
    if (value.signum() <= 0 || value.compareTo(outstanding()) > 0) {
      throw new BusinessRuleException(
          "INVALID_SETTLEMENT",
          "Settlement " + value + " exceeds outstanding " + outstanding() + " of " + documentNo);
    }
    settledAmount = settledAmount.add(value);
    status =
        outstanding().signum() == 0 ? OpenItemStatus.SETTLED : OpenItemStatus.PARTIALLY_SETTLED;
  }

  /** Writes off the remaining balance (e.g. small differences, bad debts after approval). */
  public void writeOff() {
    status = OpenItemStatus.WRITTEN_OFF;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public ItemDirection getDirection() {
    return direction;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public LocalDate getDocumentDate() {
    return documentDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getSettledAmount() {
    return settledAmount;
  }

  public OpenItemStatus getStatus() {
    return status;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getNarration() {
    return narration;
  }
}
