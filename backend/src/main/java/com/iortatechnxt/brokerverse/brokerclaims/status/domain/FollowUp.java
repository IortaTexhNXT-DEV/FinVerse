package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A follow-up recorded on a claim with the fields of the BRD scope (FRS BRD-07 FRCLM.009.04): type,
 * action taken today, next action plan summary and detail, status as of the date, settlement
 * amount, adjuster's evaluation and insurer's deductible. The turnaround dates are change request
 * CR-CLM-109.
 */
@Entity
@Table(name = "bcl_follow_up")
public class FollowUp extends BaseEntity {

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "follow_up_type", nullable = false, length = 40, updatable = false)
  private String followUpType;

  @Column(name = "action_taken", nullable = false, length = 2000, updatable = false)
  private String actionTaken;

  @Column(name = "next_plan_summary", length = 500, updatable = false)
  private String nextPlanSummary;

  @Column(name = "next_plan_detail", length = 2000, updatable = false)
  private String nextPlanDetail;

  @Column(name = "status_code", nullable = false, length = 40, updatable = false)
  private String statusCode;

  @Column(name = "settlement_amount", precision = 19, scale = 2, updatable = false)
  private BigDecimal settlementAmount;

  @Column(name = "adjuster_evaluation", precision = 19, scale = 2, updatable = false)
  private BigDecimal adjusterEvaluation;

  @Column(name = "insurer_deductible", precision = 19, scale = 2, updatable = false)
  private BigDecimal insurerDeductible;

  @Column(name = "followed_up_on", nullable = false, updatable = false)
  private LocalDate followedUpOn;

  protected FollowUp() {}

  /**
   * A follow-up.
   *
   * @param claimId claim
   * @param entry what was done and planned
   * @param amounts amounts of the follow-up
   * @param statusCode status of the claim as of the date
   * @param on business date
   */
  public FollowUp(Long claimId, Entry entry, Amounts amounts, String statusCode, LocalDate on) {
    this.claimId = claimId;
    this.followUpType = entry.type();
    this.actionTaken = entry.actionTaken();
    this.nextPlanSummary = entry.nextPlanSummary();
    this.nextPlanDetail = entry.nextPlanDetail();
    this.settlementAmount = amounts.settlementAmount();
    this.adjusterEvaluation = amounts.adjusterEvaluation();
    this.insurerDeductible = amounts.insurerDeductible();
    this.statusCode = statusCode;
    this.followedUpOn = on;
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getFollowUpType() {
    return followUpType;
  }

  public String getActionTaken() {
    return actionTaken;
  }

  public String getNextPlanSummary() {
    return nextPlanSummary;
  }

  public String getNextPlanDetail() {
    return nextPlanDetail;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public BigDecimal getSettlementAmount() {
    return settlementAmount;
  }

  public BigDecimal getAdjusterEvaluation() {
    return adjusterEvaluation;
  }

  public BigDecimal getInsurerDeductible() {
    return insurerDeductible;
  }

  public LocalDate getFollowedUpOn() {
    return followedUpOn;
  }

  /**
   * What was done and what is planned.
   *
   * @param type follow-up type ({@code BCL_FOLLOW_UP_TYPE})
   * @param actionTaken action taken today
   * @param nextPlanSummary next action plan summary
   * @param nextPlanDetail next action plan detail
   */
  public record Entry(
      String type, String actionTaken, String nextPlanSummary, String nextPlanDetail) {}

  /**
   * Amounts of a follow-up.
   *
   * @param settlementAmount settlement amount
   * @param adjusterEvaluation adjuster's evaluation
   * @param insurerDeductible insurer's deductible
   */
  public record Amounts(
      BigDecimal settlementAmount, BigDecimal adjusterEvaluation, BigDecimal insurerDeductible) {}
}
