package com.iortatechnxt.brokerverse.frbs.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A service-fee run (FRBS 2.10.0; design 5.4, 7.3): the service fee of the invoices fully paid in a
 * period, one line per service-fee segment, unit and currency. Workflow {@code FRBS_SERVICE_FEE}:
 * COMPUTED -&gt; FOR_APPROVAL -&gt; APPROVED (accrued and sent to Disbursement) -&gt; RELEASED
 * -&gt; LIQUIDATED; cancelled before approval.
 */
@Entity
@Table(name = "frbs_service_fee_run")
public class ServiceFeeRun extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Column(name = "period_from", nullable = false, updatable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false, updatable = false)
  private LocalDate periodTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RunStage stage = RunStage.COMPUTED;

  @Column(name = "invoice_count", nullable = false)
  private int invoiceCount;

  @Column(name = "fee_total", nullable = false, precision = 19, scale = 2)
  private BigDecimal feeTotal = BigDecimal.ZERO;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  protected ServiceFeeRun() {}

  /**
   * A new run.
   *
   * @param companyId company
   * @param runNo run number
   * @param from first day of the period
   * @param to last day of the period
   * @param at computation time
   */
  public ServiceFeeRun(Long companyId, String runNo, LocalDate from, LocalDate to, Instant at) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.periodFrom = from;
    this.periodTo = to;
    this.computedAt = at;
  }

  /**
   * Records the totals of a (re)computation.
   *
   * @param invoices invoices counted
   * @param fee total fee
   * @param at computation time
   */
  public void computed(int invoices, BigDecimal fee, Instant at) {
    requireStage(RunStage.COMPUTED, "recomputed");
    invoiceCount = invoices;
    feeTotal = fee;
    computedAt = at;
  }

  /**
   * Records who submitted the run.
   *
   * @param user submitter
   * @param at time
   */
  public void submitted(String user, Instant at) {
    submittedBy = user;
    submittedAt = at;
  }

  /**
   * Records who approved the run.
   *
   * @param user approver
   * @param at time
   */
  public void approved(String user, Instant at) {
    approvedBy = user;
    approvedAt = at;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param newStage stage
   */
  public void moveTo(RunStage newStage) {
    stage = newStage;
  }

  /**
   * Refuses an action outside a stage.
   *
   * @param expected stage the action needs
   * @param action what is attempted
   */
  public void requireStage(RunStage expected, String action) {
    if (stage != expected) {
      throw new BusinessRuleException(
          "SERVICE_FEE_STAGE", "Run " + runNo + " is " + stage + " and cannot be " + action);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public RunStage getStage() {
    return stage;
  }

  public int getInvoiceCount() {
    return invoiceCount;
  }

  public BigDecimal getFeeTotal() {
    return feeTotal;
  }

  public Instant getComputedAt() {
    return computedAt;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }
}
