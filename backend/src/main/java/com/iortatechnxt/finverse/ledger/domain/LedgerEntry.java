package com.iortatechnxt.finverse.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable posted GL transaction line — the book of record.
 *
 * <p>Written only by the posting engine. There are no setters and no update/delete paths; errors
 * are corrected by reversal journals. Debit and credit are stored in separate columns (both in
 * transaction and base currency) so aggregation is a simple SUM.
 */
@Entity
@Table(name = "gl_ledger_entry")
public class LedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(name = "period_id", nullable = false, updatable = false)
  private Long periodId;

  @Column(name = "value_date", nullable = false, updatable = false)
  private LocalDate valueDate;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "batch_no", nullable = false, updatable = false, length = 40)
  private String batchNo;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "journal_type", nullable = false, updatable = false, length = 20)
  private String journalType;

  @Column(nullable = false, updatable = false, length = 3)
  private String currency;

  @Column(name = "debit_fc", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal debitFc;

  @Column(name = "credit_fc", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal creditFc;

  @Column(name = "debit_base", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal debitBase;

  @Column(name = "credit_base", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal creditBase;

  @Column(name = "cost_center", updatable = false, length = 20)
  private String costCenter;

  @Column(name = "business_line", updatable = false, length = 20)
  private String businessLine;

  @Column(name = "party_code", updatable = false, length = 30)
  private String partyCode;

  @Column(updatable = false, length = 60)
  private String reference;

  @Column(updatable = false, length = 250)
  private String narration;

  @Column(name = "posted_at", nullable = false, updatable = false)
  private Instant postedAt;

  @Column(name = "posted_by", nullable = false, updatable = false, length = 50)
  private String postedBy;

  protected LedgerEntry() {}

  /**
   * Creates an entry from its values.
   *
   * @param v values
   */
  public LedgerEntry(LedgerEntryValues v) {
    this.companyId = v.companyId();
    this.branchId = v.branchId();
    this.accountId = v.accountId();
    this.periodId = v.periodId();
    this.valueDate = v.valueDate();
    this.batchId = v.batchId();
    this.batchNo = v.batchNo();
    this.lineNo = v.lineNo();
    this.journalType = v.journalType();
    this.currency = v.currency();
    this.debitFc = v.debitFc();
    this.creditFc = v.creditFc();
    this.debitBase = v.debitBase();
    this.creditBase = v.creditBase();
    this.costCenter = v.costCenter();
    this.businessLine = v.businessLine();
    this.partyCode = v.partyCode();
    this.reference = v.reference();
    this.narration = v.narration();
    this.postedAt = v.postedAt();
    this.postedBy = v.postedBy();
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public Long getPeriodId() {
    return periodId;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public Long getBatchId() {
    return batchId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getJournalType() {
    return journalType;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getDebitFc() {
    return debitFc;
  }

  public BigDecimal getCreditFc() {
    return creditFc;
  }

  public BigDecimal getDebitBase() {
    return debitBase;
  }

  public BigDecimal getCreditBase() {
    return creditBase;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getReference() {
    return reference;
  }

  public String getNarration() {
    return narration;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }
}
