package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A reconciliation cycle: one insurer and one production month (OPERATIONS_DESIGN 7, workflow
 * {@code OPS_RECON}): register extracted, sent to the insurer, insurer feedback uploaded and
 * reconciled, closed. The stage mirrors the work case; once closed, a new cycle can be opened for
 * the same insurer and month.
 */
@Entity
@Table(name = "prc_cycle")
public class ReconCycle extends BaseEntity {

  /** Workflow of the cycles. */
  public static final String WORKFLOW = "OPS_RECON";

  /** First stage. */
  public static final String EXTRACTED = "EXTRACTED";

  /** Register sent, waiting for the insurer. */
  public static final String SENT = "SENT_TO_INSURER";

  /** Insurer feedback uploaded. */
  public static final String RECONCILING = "RECONCILING";

  /** Terminal stage. */
  public static final String CLOSED = "CLOSED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "cycle_no", nullable = false, length = 60, updatable = false)
  private String cycleNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "production_month", nullable = false, updatable = false)
  private LocalDate productionMonth;

  @Column(nullable = false, length = 40)
  private String stage;

  @Column(nullable = false)
  private boolean closed;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "last_upload_at")
  private Instant lastUploadAt;

  @Column(name = "last_matched_at")
  private Instant lastMatchedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected ReconCycle() {}

  /**
   * Opens a cycle.
   *
   * @param companyId company
   * @param cycleNo cycle number
   * @param insurerCode insurer
   * @param productionMonth first day of the production month
   */
  public ReconCycle(Long companyId, String cycleNo, String insurerCode, LocalDate productionMonth) {
    this.companyId = companyId;
    this.cycleNo = cycleNo;
    this.insurerCode = insurerCode;
    this.productionMonth = productionMonth.withDayOfMonth(1);
    this.stage = EXTRACTED;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param stageCode new stage
   * @param at time of the change
   */
  public void mirrorStage(String stageCode, Instant at) {
    this.stage = stageCode;
    if (CLOSED.equals(stageCode) && !closed) {
      this.closed = true;
      this.closedAt = at;
    }
  }

  /**
   * Records that the register was sent.
   *
   * @param at time sent
   */
  public void sent(Instant at) {
    this.sentAt = at;
  }

  /**
   * Records an insurer upload.
   *
   * @param at time uploaded
   */
  public void uploaded(Instant at) {
    this.lastUploadAt = at;
  }

  /**
   * Records a matching run.
   *
   * @param at time matched
   */
  public void matched(Instant at) {
    this.lastMatchedAt = at;
  }

  /**
   * Whether the cycle can still change.
   *
   * @return true until closed
   */
  public boolean isOpen() {
    return !closed;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCycleNo() {
    return cycleNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public LocalDate getProductionMonth() {
    return productionMonth;
  }

  public String getStage() {
    return stage;
  }

  public boolean isClosed() {
    return closed;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getLastUploadAt() {
    return lastUploadAt;
  }

  public Instant getLastMatchedAt() {
    return lastMatchedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
