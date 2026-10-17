package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A loan of a loan file of the bank (BRIDSP-02/13; FR-SP-035): the loan report (LAMD, LMS or LAD),
 * the PN number or the loan application number, the loan status, the amortised flag, the maturity
 * date, the branch and segment, and the collateral numbers the matching step compares with the
 * policy.
 */
@Entity
@Table(name = "sbm_lamd_loan")
public class SbmLamdLoan extends BaseEntity {

  /** Loan report of the rows loaded before the other reports existed. */
  public static final String DEFAULT_REPORT = "LAMD";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "snapshot_date", nullable = false, updatable = false)
  private LocalDate snapshotDate;

  @Column(name = "loan_report", nullable = false, updatable = false, length = 10)
  private String loanReport = DEFAULT_REPORT;

  @Column(name = "pn_no", updatable = false, length = 40)
  private String pnNo;

  @Column(name = "loan_application_no", updatable = false, length = 40)
  private String loanApplicationNo;

  @Column(length = 60)
  private String branch;

  @Column(length = 30)
  private String segment;

  @Column(name = "borrower_name", length = 250)
  private String borrowerName;

  @Column(name = "loan_status", nullable = false, length = 20)
  private String loanStatus;

  @Column(nullable = false)
  private boolean amortised;

  @Column(name = "maturity_date")
  private LocalDate maturityDate;

  @Column(name = "originating_unit", length = 60)
  private String originatingUnit;

  @Column(precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(name = "serial_no", length = 60)
  private String serialNo;

  @Column(name = "motor_no", length = 60)
  private String motorNo;

  @Column(name = "bulk_job_no", length = 30)
  private String bulkJobNo;

  protected SbmLamdLoan() {}

  /**
   * A loan row.
   *
   * @param companyId company
   * @param snapshotDate snapshot date
   * @param pnNo PN
   * @param facts loan facts
   * @param bulkJobNo upload
   */
  public SbmLamdLoan(
      Long companyId, LocalDate snapshotDate, String pnNo, LoanFacts facts, String bulkJobNo) {
    this(companyId, new LoanKey(DEFAULT_REPORT, snapshotDate, pnNo, null), facts, bulkJobNo);
  }

  /**
   * A row of a loan file.
   *
   * @param companyId company
   * @param key loan report, date of the file, PN number and loan application number
   * @param facts loan facts
   * @param bulkJobNo upload
   */
  public SbmLamdLoan(Long companyId, LoanKey key, LoanFacts facts, String bulkJobNo) {
    this.companyId = companyId;
    this.loanReport = key.loanReport();
    this.snapshotDate = key.snapshotDate();
    this.pnNo = key.pnNo();
    this.loanApplicationNo = key.loanApplicationNo();
    this.bulkJobNo = bulkJobNo;
    update(facts);
  }

  /**
   * Sets the branch and segment of the loan.
   *
   * @param branch branch, may be null
   * @param segment segment, may be null
   */
  public void placeOf(String branch, String segment) {
    this.branch = branch;
    this.segment = segment;
  }

  /**
   * Replaces the facts (the same PN loaded again for the same date).
   *
   * @param facts loan facts
   */
  public final void update(LoanFacts facts) {
    this.borrowerName = facts.borrowerName();
    this.loanStatus = facts.loanStatus();
    this.amortised = facts.amortised();
    this.maturityDate = facts.maturityDate();
    this.originatingUnit = facts.originatingUnit();
    this.balance = facts.balance();
    this.serialNo = facts.serialNo();
    this.motorNo = facts.motorNo();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  public String getLoanReport() {
    return loanReport;
  }

  public String getLoanApplicationNo() {
    return loanApplicationNo;
  }

  public String getBranch() {
    return branch;
  }

  public String getSegment() {
    return segment;
  }

  public String getPnNo() {
    return pnNo;
  }

  public String getBorrowerName() {
    return borrowerName;
  }

  public String getLoanStatus() {
    return loanStatus;
  }

  public boolean isAmortised() {
    return amortised;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public String getOriginatingUnit() {
    return originatingUnit;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public String getSerialNo() {
    return serialNo;
  }

  public String getMotorNo() {
    return motorNo;
  }

  public String getBulkJobNo() {
    return bulkJobNo;
  }

  /**
   * Identity of a row of a loan file.
   *
   * @param loanReport loan report (list SBM_LOAN_REPORT)
   * @param snapshotDate date of the file
   * @param pnNo PN number, may be null when the loan application number is given
   * @param loanApplicationNo loan application number, may be null
   */
  public record LoanKey(
      String loanReport, LocalDate snapshotDate, String pnNo, String loanApplicationNo) {}

  /**
   * What a loan file says of a loan.
   *
   * @param borrowerName borrower
   * @param loanStatus loan status
   * @param amortised amortised
   * @param maturityDate maturity date
   * @param originatingUnit originating unit
   * @param balance balance
   * @param serialNo collateral serial number
   * @param motorNo collateral motor number
   */
  public record LoanFacts(
      String borrowerName,
      String loanStatus,
      boolean amortised,
      LocalDate maturityDate,
      String originatingUnit,
      BigDecimal balance,
      String serialNo,
      String motorNo) {}
}
