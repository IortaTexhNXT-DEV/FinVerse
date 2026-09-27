package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A loan of a LAMD snapshot (BRIDSP-13): per PN the loan status, the amortised flag, the maturity
 * date and the collateral numbers the matching step compares with the policy.
 */
@Entity
@Table(name = "sbm_lamd_loan")
public class SbmLamdLoan extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "snapshot_date", nullable = false, updatable = false)
  private LocalDate snapshotDate;

  @Column(name = "pn_no", nullable = false, updatable = false, length = 40)
  private String pnNo;

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
    this.companyId = companyId;
    this.snapshotDate = snapshotDate;
    this.pnNo = pnNo;
    this.bulkJobNo = bulkJobNo;
    update(facts);
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
   * Facts of a loan.
   *
   * @param borrowerName borrower
   * @param loanStatus ACTIVE, OPEN_MARKET, FULLY_PAID or REMEDIAL
   * @param amortised amortised
   * @param maturityDate maturity
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
