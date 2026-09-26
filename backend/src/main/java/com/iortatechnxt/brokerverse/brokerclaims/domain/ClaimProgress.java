package com.iortatechnxt.brokerverse.brokerclaims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Where a claim stands (BRCLM.005/010-015/017-021/029/035; CLAIMS_BROKING_DESIGN 5.1 and 8): the
 * BDOI status and the time it was set (age this stage), the phase, the closure, the requested type
 * of settlement with amount and date, the adjuster, the next follow-up date and the next action
 * plan. A new claim starts in phase NEW without a status; the first status is set at recording.
 *
 * <p>Owned by wave CL1-B. The mutators are called only by the status engine of {@code
 * brokerclaims.status} (status access matrix, phase change and workflow, settlement, closure,
 * reopen, follow-up, action plan, adjuster), which writes the history and publishes {@link
 * ClaimStatusChanged}. Other waves read the progress only through the getters (CL1-A's {@code
 * ClaimsFeed} adapter). Mapped by CL0 to the V1021 columns of {@code bcl_claim}.
 */
@Embeddable
public class ClaimProgress {

  @Column(name = "status_code", length = 40)
  private String statusCode;

  @Column(name = "status_since")
  private Instant statusSince;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ClaimPhase phase;

  @Enumerated(EnumType.STRING)
  @Column(name = "closure_kind", length = 12)
  private ClaimClosureKind closureKind;

  @Column(name = "settlement_type_code", length = 40)
  private String settlementTypeCode;

  @Column(name = "settlement_amount", precision = 19, scale = 2)
  private BigDecimal settlementAmount;

  @Column(name = "date_settled")
  private LocalDate dateSettled;

  @Column(name = "closed_on")
  private LocalDate closedOn;

  @Column(name = "adjuster_code", length = 40)
  private String adjusterCode;

  @Column(name = "next_follow_up_date")
  private LocalDate nextFollowUpDate;

  @Column(name = "follow_up_overridden", nullable = false)
  private boolean followUpOverridden;

  @Column(name = "next_action_plan", length = 2000)
  private String nextActionPlan;

  protected ClaimProgress() {}

  /**
   * Progress of a newly recorded claim: phase NEW, no status, no follow-up yet.
   *
   * @return progress
   */
  static ClaimProgress newClaim() {
    ClaimProgress progress = new ClaimProgress();
    progress.phase = ClaimPhase.NEW;
    return progress;
  }

  /**
   * Sets a BDOI status and moves the claim to its phase (BRCLM.011/035): "age this stage" restarts
   * and a status of phase TEMP_CLOSED marks a temporary closure. A CLOSED claim is reopened first,
   * and the phase CLOSED is reached only through a closing settlement type ({@link #close}).
   *
   * @param statusCode status ({@code BCL_CLAIM_STATUS})
   * @param statusPhase phase of the status (never CLOSED)
   * @param at time of the change
   */
  public void changeStatus(String statusCode, ClaimPhase statusPhase, Instant at) {
    if (phase == ClaimPhase.CLOSED || statusPhase == ClaimPhase.CLOSED) {
      throw new IllegalStateException("A status change never enters or leaves phase CLOSED");
    }
    this.statusCode = statusCode;
    this.statusSince = at;
    this.phase = statusPhase;
    this.closureKind = statusPhase == ClaimPhase.TEMP_CLOSED ? ClaimClosureKind.TEMPORARY : null;
  }

  /**
   * Sets the next follow-up date computed at a status change (BRCLM.019). An override is kept until
   * its date passes (FR-CL-050 R2); a passed override gives way to the computed date.
   *
   * @param computed date computed from the status follow-up days
   * @param today business date
   * @return true when the date changed
   */
  public boolean scheduleFollowUp(LocalDate computed, LocalDate today) {
    if (followUpOverridden && nextFollowUpDate != null && !nextFollowUpDate.isBefore(today)) {
      return false;
    }
    boolean changed = !computed.equals(nextFollowUpDate);
    this.nextFollowUpDate = computed;
    this.followUpOverridden = false;
    return changed;
  }

  /**
   * Overrides the next follow-up date (BRCLM.019, BCL_FOLLOW_UP_OVERRIDE).
   *
   * @param date new date (today or later, checked by the service)
   */
  public void overrideFollowUp(LocalDate date) {
    this.nextFollowUpDate = date;
    this.followUpOverridden = true;
  }

  /**
   * Encodes the next action plan summary (BRCLM.020/021).
   *
   * @param text up to 2,000 characters, null clears it
   */
  public void planNextAction(String text) {
    this.nextActionPlan = text;
  }

  /**
   * Sets or clears the adjuster / appraiser of the claim (BRCLM.018).
   *
   * @param code adjuster ({@code BCL_ADJUSTER}), null clears it
   */
  public void assignAdjuster(String code) {
    this.adjusterCode = code;
  }

  /**
   * Records the requested type of settlement with the insurer's amount and date (BRCLM.015/029).
   *
   * @param typeCode settlement type ({@code BCL_SETTLEMENT_TYPE})
   * @param amount settlement amount, null when the type needs none
   * @param settledOn date settled, null when the type needs none
   */
  public void settle(String typeCode, BigDecimal amount, LocalDate settledOn) {
    this.settlementTypeCode = typeCode;
    this.settlementAmount = amount;
    this.dateSettled = settledOn;
  }

  /**
   * Closes the claim permanently through a closing settlement type (BRCLM.005/035): it stops ageing
   * and accepts only diary entries, insurer updates and reopen.
   *
   * @param on closure date
   */
  public void close(LocalDate on) {
    this.phase = ClaimPhase.CLOSED;
    this.closureKind = ClaimClosureKind.PERMANENT;
    this.closedOn = on;
  }

  /**
   * Reopens a permanently closed claim (BRCLM.035, CLQ06): the claim is in progress again, the
   * status is kept, and the settlement is cleared (it stays in the claim history).
   *
   * @param at time of the reopen ("age this stage" restarts)
   */
  public void reopen(Instant at) {
    this.phase = ClaimPhase.IN_PROGRESS;
    this.closureKind = null;
    this.closedOn = null;
    this.statusSince = at;
    settle(null, null, null);
  }

  public String getStatusCode() {
    return statusCode;
  }

  public Instant getStatusSince() {
    return statusSince;
  }

  public ClaimPhase getPhase() {
    return phase;
  }

  public ClaimClosureKind getClosureKind() {
    return closureKind;
  }

  public String getSettlementTypeCode() {
    return settlementTypeCode;
  }

  public BigDecimal getSettlementAmount() {
    return settlementAmount;
  }

  public LocalDate getDateSettled() {
    return dateSettled;
  }

  public LocalDate getClosedOn() {
    return closedOn;
  }

  public String getAdjusterCode() {
    return adjusterCode;
  }

  public LocalDate getNextFollowUpDate() {
    return nextFollowUpDate;
  }

  public boolean isFollowUpOverridden() {
    return followUpOverridden;
  }

  public String getNextActionPlan() {
    return nextActionPlan;
  }
}
