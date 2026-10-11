package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The matching of a record with the loan files in a processing run (FR-SP-035): the loan file used
 * (report, date and upload), the key that matched, or the reason the record is unmatched, with the
 * time stamp. The categorisation of the record is in the run results of the same run.
 */
@Entity
@Table(name = "sbm_loan_match")
public class SbmLoanMatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "matched_at", nullable = false, updatable = false)
  private Instant matchedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 10)
  private Outcome outcome;

  @Enumerated(EnumType.STRING)
  @Column(name = "key_used", updatable = false, length = 20)
  private MatchKey keyUsed;

  @Column(name = "loan_id", updatable = false)
  private Long loanId;

  @Column(name = "loan_report", updatable = false, length = 10)
  private String loanReport;

  @Column(name = "snapshot_date", updatable = false)
  private LocalDate snapshotDate;

  @Column(name = "bulk_job_no", updatable = false, length = 30)
  private String bulkJobNo;

  @Column(updatable = false, length = 200)
  private String reason;

  protected SbmLoanMatch() {}

  /**
   * A record matched with a loan.
   *
   * @param run run and record
   * @param when time stamp
   * @param key key that matched
   * @param loan loan row
   * @return log row
   */
  public static SbmLoanMatch matched(RunRecord run, Instant when, MatchKey key, SbmLamdLoan loan) {
    SbmLoanMatch m = new SbmLoanMatch(run, when, Outcome.MATCHED);
    m.keyUsed = key;
    m.loanId = loan.getId();
    m.loanReport = loan.getLoanReport();
    m.snapshotDate = loan.getSnapshotDate();
    m.bulkJobNo = loan.getBulkJobNo();
    return m;
  }

  /**
   * A record no loan file matches.
   *
   * @param run run and record
   * @param when time stamp
   * @param reason why
   * @return log row
   */
  public static SbmLoanMatch unmatched(RunRecord run, Instant when, String reason) {
    SbmLoanMatch m = new SbmLoanMatch(run, when, Outcome.UNMATCHED);
    m.reason = reason;
    return m;
  }

  private SbmLoanMatch(RunRecord run, Instant when, Outcome outcome) {
    this.companyId = run.companyId();
    this.runId = run.runId();
    this.policyId = run.policyId();
    this.matchedAt = when;
    this.outcome = outcome;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getRunId() {
    return runId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public Instant getMatchedAt() {
    return matchedAt;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public MatchKey getKeyUsed() {
    return keyUsed;
  }

  public Long getLoanId() {
    return loanId;
  }

  public String getLoanReport() {
    return loanReport;
  }

  public LocalDate getSnapshotDate() {
    return snapshotDate;
  }

  public String getBulkJobNo() {
    return bulkJobNo;
  }

  public String getReason() {
    return reason;
  }

  /** Result of the matching. */
  public enum Outcome {
    /** A loan file holds the loan of the record. */
    MATCHED,
    /** No loan file holds it. */
    UNMATCHED
  }

  /** Key that matched, in the order the matching tries them. */
  public enum MatchKey {
    /** Promissory note number. */
    PN,
    /** Loan application number. */
    LOAN_APPLICATION
  }

  /**
   * The run and record of a log row.
   *
   * @param companyId company
   * @param runId processing run
   * @param policyId record
   */
  public record RunRecord(Long companyId, Long runId, Long policyId) {}
}
