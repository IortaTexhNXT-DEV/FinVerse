package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A receipt number series of a branch (CSHID.006/015, OQ05): AR per branch, OR for Head Office
 * only. Numbers are allocated sequentially under a row lock and a depleted series refuses
 * allocation. Maker-checker: a series is used only once authorized.
 */
@Entity
@Table(name = "csh_receipt_series")
public class ReceiptSeries extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 2, updatable = false)
  private ReceiptKind kind;

  @Column(name = "atp_no", length = 40)
  private String atpNo;

  @Column(nullable = false, length = 20, updatable = false)
  private String prefix;

  @Column(name = "from_no", nullable = false, updatable = false)
  private long fromNo;

  @Column(name = "to_no", nullable = false)
  private long toNo;

  @Column(name = "next_no", nullable = false)
  private long nextNo;

  @Column(name = "warn_at", nullable = false)
  private int warnAt;

  protected ReceiptSeries() {}

  /**
   * Creates a series (pending authorization).
   *
   * @param companyId company
   * @param branchId branch
   * @param kind AR or OR
   * @param prefix number prefix, e.g. AR-HO-
   * @param firstNo first number
   * @param lastNo last number
   */
  public ReceiptSeries(
      Long companyId, Long branchId, ReceiptKind kind, String prefix, long firstNo, long lastNo) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.kind = kind;
    this.prefix = prefix;
    this.fromNo = firstNo;
    this.toNo = lastNo;
    this.nextNo = firstNo;
    if (toNo < fromNo) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_RANGE", "The last number must not be below the first number");
    }
  }

  /**
   * Updates the editable details; the series must be authorized again.
   *
   * @param atp BIR authority to print
   * @param lastNo new last number (extension of the range)
   * @param warningAt remaining numbers that raise the low-series alert
   */
  public void update(String atp, long lastNo, int warningAt) {
    if (lastNo < nextNo - 1 || lastNo < fromNo) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_RANGE", "The last number cannot be below the numbers already issued");
    }
    this.atpNo = atp;
    this.toNo = lastNo;
    this.warnAt = warningAt;
    markModified();
  }

  /**
   * Allocates the next number (CSHID.006); refuses a depleted series (CSHID.015).
   *
   * @return formatted receipt number
   */
  public String allocate() {
    if (isDepleted()) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_DEPLETED", "Receipt series " + prefix + " is depleted");
    }
    long number = nextNo;
    nextNo = nextNo + 1;
    String digits = Long.toString(number);
    int width = String.valueOf(toNo).length();
    return prefix + "0".repeat(Math.max(0, width - digits.length())) + digits;
  }

  /**
   * Whether no number is left.
   *
   * @return true when depleted
   */
  public boolean isDepleted() {
    return nextNo > toNo;
  }

  /**
   * Numbers left.
   *
   * @return remaining count
   */
  public long remaining() {
    return Math.max(0, toNo - nextNo + 1);
  }

  /**
   * Whether the remaining count reached the warning threshold.
   *
   * @return true when low
   */
  public boolean isLow() {
    return remaining() <= warnAt;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public ReceiptKind getKind() {
    return kind;
  }

  public String getAtpNo() {
    return atpNo;
  }

  public String getPrefix() {
    return prefix;
  }

  public long getFromNo() {
    return fromNo;
  }

  public long getToNo() {
    return toNo;
  }

  public long getNextNo() {
    return nextNo;
  }

  public int getWarnAt() {
    return warnAt;
  }
}
