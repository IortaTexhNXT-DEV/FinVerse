package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.RunStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A computed incentive run of a scheme for a period (CMRID.003/005/006): eligible production,
 * exclusions, the tier applied, the incentive, the pass-on to branches, and once posted the accrual
 * (OPS_INCENTIVE_ACCRUE) and pass-on (OPS_INCENTIVE_PASS_ON) journals.
 */
@Entity
@Table(name = "cmr_incentive_run")
public class IncentiveRun extends BaseEntity {

  private static final int MAX_REFS = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Column(name = "scheme_id", nullable = false, updatable = false)
  private Long schemeId;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RunStatus status;

  @Column(name = "eligible_count", nullable = false)
  private int eligibleCount;

  @Column(name = "eligible_production", nullable = false, precision = 19, scale = 2)
  private BigDecimal eligibleProduction = BigDecimal.ZERO;

  @Column(name = "excluded_count", nullable = false)
  private int excludedCount;

  @Column(name = "excluded_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal excludedAmount = BigDecimal.ZERO;

  @Column(name = "tier_applied", length = 200)
  private String tierApplied;

  @Column(name = "incentive_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal incentiveAmount = BigDecimal.ZERO;

  @Column(name = "pass_on_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal passOnAmount = BigDecimal.ZERO;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "posted_by", length = 50)
  private String postedBy;

  @Column(name = "journal_refs", length = MAX_REFS)
  private String journalRefs;

  protected IncentiveRun() {}

  /**
   * A new run.
   *
   * @param companyId company
   * @param runNo run number
   * @param schemeId scheme
   * @param periodFrom first day
   * @param periodTo last day
   */
  public IncentiveRun(
      Long companyId, String runNo, Long schemeId, LocalDate periodFrom, LocalDate periodTo) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.schemeId = schemeId;
    this.periodFrom = periodFrom;
    this.periodTo = periodTo;
    this.status = RunStatus.COMPUTED;
  }

  /**
   * Records the computed totals.
   *
   * @param totals totals
   */
  public void computed(Totals totals) {
    this.eligibleCount = totals.eligibleCount();
    this.eligibleProduction = totals.eligibleProduction();
    this.excludedCount = totals.excludedCount();
    this.excludedAmount = totals.excludedAmount();
    this.tierApplied = totals.tierApplied();
    this.incentiveAmount = totals.incentive();
    this.passOnAmount = totals.passOn();
  }

  /**
   * Records the posting.
   *
   * @param at time
   * @param by user
   * @param refs journal and disbursement references
   */
  public void posted(Instant at, String by, String refs) {
    requireComputed();
    this.status = RunStatus.POSTED;
    this.postedAt = at;
    this.postedBy = by;
    this.journalRefs =
        refs == null || refs.length() <= MAX_REFS ? refs : refs.substring(0, MAX_REFS);
  }

  /** Cancels a computed run. */
  public void cancel() {
    requireComputed();
    this.status = RunStatus.CANCELLED;
  }

  private void requireComputed() {
    if (status != RunStatus.COMPUTED) {
      throw new BusinessRuleException(
          "INCENTIVE_RUN_STATE", "Run " + runNo + " is " + status + " and cannot change");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public Long getSchemeId() {
    return schemeId;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public RunStatus getStatus() {
    return status;
  }

  public int getEligibleCount() {
    return eligibleCount;
  }

  public BigDecimal getEligibleProduction() {
    return eligibleProduction;
  }

  public int getExcludedCount() {
    return excludedCount;
  }

  public BigDecimal getExcludedAmount() {
    return excludedAmount;
  }

  public String getTierApplied() {
    return tierApplied;
  }

  public BigDecimal getIncentiveAmount() {
    return incentiveAmount;
  }

  public BigDecimal getPassOnAmount() {
    return passOnAmount;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }

  public String getJournalRefs() {
    return journalRefs;
  }

  /**
   * Totals of a computation.
   *
   * @param eligibleCount eligible invoices
   * @param eligibleProduction eligible production
   * @param excludedCount excluded invoices
   * @param excludedAmount excluded production
   * @param tierApplied description of the tier applied
   * @param incentive incentive
   * @param passOn amount passed on to branches
   */
  public record Totals(
      int eligibleCount,
      BigDecimal eligibleProduction,
      int excludedCount,
      BigDecimal excludedAmount,
      String tierApplied,
      BigDecimal incentive,
      BigDecimal passOn) {}
}
