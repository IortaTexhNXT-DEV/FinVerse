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
 * <p>Owned by wave CL1-B, which adds the status engine (status access matrix, phase change,
 * settlement, closure, reopen, follow-up). Other waves read the status only through the getters
 * (CL1-A's {@code ClaimsFeed} adapter). Mapped by CL0 to the V1021 columns of {@code bcl_claim}.
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
