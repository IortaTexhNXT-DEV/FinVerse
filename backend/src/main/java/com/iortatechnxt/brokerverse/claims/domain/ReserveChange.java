package com.iortatechnxt.brokerverse.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Request to set a new estimate for one side and cost type of a claim (e.g. "loss payment estimate
 * becomes 250,000"). The change against the estimate current at approval becomes a typed movement
 * line: increases are type 1 / 2, decreases type 3 / 4 (Reports Book).
 *
 * <p>System-generated changes release the outstanding reserve when a claim is finally settled,
 * closed, repudiated or withdrawn; they are approved as part of that decision.
 */
@Entity
@Table(name = "clm_reserve_change")
public class ReserveChange extends ClaimDocument {

  @Column(name = "change_no", nullable = false)
  private int changeNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EstimateSide side;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 10)
  private CostType costType;

  @Column(name = "new_estimate", nullable = false, precision = 19, scale = 2)
  private BigDecimal newEstimate;

  @Column(name = "previous_estimate", precision = 19, scale = 2)
  private BigDecimal previousEstimate;

  @Column(name = "change_amount", precision = 19, scale = 2)
  private BigDecimal changeAmount;

  @Column(name = "our_change", precision = 19, scale = 2)
  private BigDecimal ourChange;

  @Column(nullable = false, length = 200)
  private String reason;

  @Column(name = "system_generated", nullable = false)
  private boolean systemGenerated;

  protected ReserveChange() {}

  /**
   * Creates a reserve change.
   *
   * @param claim claim
   * @param changeNo sequence within the claim
   * @param line side, cost type and new estimate
   * @param reason reason for the change
   * @param approval maker-checker state (pending, or approved for system releases)
   */
  public ReserveChange(
      Claim claim, int changeNo, EstimateLine line, String reason, ClaimApproval approval) {
    super(claim, approval);
    this.changeNo = changeNo;
    this.side = line.side();
    this.costType = line.costType();
    this.newEstimate = line.amount();
    this.reason = reason;
    this.systemGenerated = approval.getStatus() == DocumentStatus.APPROVED;
  }

  /**
   * Records the change applied at approval.
   *
   * @param previous estimate before the change (100 %)
   * @param ourDelta company share of the change
   */
  public void recordApplied(BigDecimal previous, BigDecimal ourDelta) {
    this.previousEstimate = previous;
    this.changeAmount = newEstimate.subtract(previous);
    this.ourChange = ourDelta;
  }

  @Override
  public String label() {
    return "Reserve change " + getClaim().getClaimNo() + "/" + changeNo;
  }

  public int getChangeNo() {
    return changeNo;
  }

  public EstimateSide getSide() {
    return side;
  }

  public CostType getCostType() {
    return costType;
  }

  public BigDecimal getNewEstimate() {
    return newEstimate;
  }

  public BigDecimal getPreviousEstimate() {
    return previousEstimate;
  }

  public BigDecimal getChangeAmount() {
    return changeAmount;
  }

  public BigDecimal getOurChange() {
    return ourChange;
  }

  public String getReason() {
    return reason;
  }

  public boolean isSystemGenerated() {
    return systemGenerated;
  }
}
