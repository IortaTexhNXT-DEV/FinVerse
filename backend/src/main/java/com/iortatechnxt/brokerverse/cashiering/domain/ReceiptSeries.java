package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

  /** Token of the sequence in a number format. */
  public static final String SEQ = "{SEQ}";

  @Embedded private RecordOrigin recordOrigin = RecordOrigin.BIBS;

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

  @Column(name = "series_year")
  private Integer seriesYear;

  @Column(name = "number_format", length = 80)
  private String numberFormat;

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
   * Continues a legacy series from the next number the legacy system would have issued (Data
   * Migration object R11).
   *
   * @param legacyNextNo next number in legacy
   */
  public void continueFrom(long legacyNextNo) {
    if (legacyNextNo < fromNo || legacyNextNo > toNo + 1) {
      throw new BusinessRuleException(
          "RECEIPT_SERIES_NEXT_OUT_OF_RANGE",
          "Next number " + legacyNextNo + " is outside " + fromNo + "-" + toNo);
    }
    this.nextNo = legacyNextNo;
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
    return format(number);
  }

  /**
   * The number the series gives next, without taking it.
   *
   * @return formatted number, null when depleted
   */
  public String preview() {
    return isDepleted() ? null : format(nextNo);
  }

  private String format(long number) {
    String digits = Long.toString(number);
    int width = String.valueOf(toNo).length();
    String sequence = "0".repeat(Math.max(0, width - digits.length())) + digits;
    if (numberFormat != null && numberFormat.contains(SEQ)) {
      return numberFormat.replace(SEQ, sequence);
    }
    return prefix + sequence;
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

  /**
   * Sets the year of the series and its number format (FRS.CSH.02.03.02 / 02.03.03): the format
   * holds the prefix, the year and the branch indicator already resolved and the token {@code
   * {SEQ}}.
   *
   * @param year year the series serves, null for every year
   * @param format number format, null for prefix and sequence
   */
  public void numbering(Integer year, String format) {
    this.seriesYear = year;
    this.numberFormat = format == null || format.isBlank() ? null : format.strip();
  }

  public Integer getSeriesYear() {
    return seriesYear;
  }

  public String getNumberFormat() {
    return numberFormat;
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

  /**
   * Marks a record loaded from a legacy system (DATA_MIGRATION_DESIGN section 10).
   *
   * @param origin source system, legacy reference and batch
   */
  public void markMigrated(RecordOrigin origin) {
    this.recordOrigin = origin;
  }

  /**
   * Where the record comes from.
   *
   * @return BIBS or the legacy origin
   */
  public RecordOrigin getRecordOrigin() {
    return recordOrigin == null ? RecordOrigin.BIBS : recordOrigin;
  }
}
