package com.iortatechnxt.finverse.claims.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Running estimate and paid totals of a claim at 100 %, maintained when documents are approved.
 *
 * <p>Invariants: payment outstanding (estimate − paid) of loss and of expense is never negative;
 * recoveries never exceed the recovery estimate. The company share of any total is derived with
 * {@link ClaimPolicy#ourShare}, so rounding never accumulates across movements.
 */
@Embeddable
public class ClaimTotals {

  @Column(name = "estimate_loss", nullable = false, precision = 19, scale = 2)
  private BigDecimal estimateLoss = Money.zero();

  @Column(name = "estimate_expense", nullable = false, precision = 19, scale = 2)
  private BigDecimal estimateExpense = Money.zero();

  @Column(name = "estimate_recovery", nullable = false, precision = 19, scale = 2)
  private BigDecimal estimateRecovery = Money.zero();

  @Column(name = "paid_loss", nullable = false, precision = 19, scale = 2)
  private BigDecimal paidLoss = Money.zero();

  @Column(name = "paid_expense", nullable = false, precision = 19, scale = 2)
  private BigDecimal paidExpense = Money.zero();

  @Column(name = "recovered", nullable = false, precision = 19, scale = 2)
  private BigDecimal recovered = Money.zero();

  /**
   * Current estimate of a side and cost type (recoveries are loss only).
   *
   * @param side side
   * @param cost cost type
   * @return estimate at 100 %
   */
  public BigDecimal estimate(EstimateSide side, CostType cost) {
    if (side == EstimateSide.RECOVERY) {
      return estimateRecovery;
    }
    return cost == CostType.LOSS ? estimateLoss : estimateExpense;
  }

  /**
   * Amount settled (payment side) or recovered (recovery side).
   *
   * @param side side
   * @param cost cost type
   * @return paid / recovered at 100 %
   */
  public BigDecimal paid(EstimateSide side, CostType cost) {
    if (side == EstimateSide.RECOVERY) {
      return recovered;
    }
    return cost == CostType.LOSS ? paidLoss : paidExpense;
  }

  /**
   * Outstanding of a side and cost type: estimate − paid.
   *
   * @param side side
   * @param cost cost type
   * @return outstanding at 100 %
   */
  public BigDecimal outstanding(EstimateSide side, CostType cost) {
    return estimate(side, cost).subtract(paid(side, cost));
  }

  /**
   * Total payment outstanding (loss and expense), i.e. the claim reserve.
   *
   * @return outstanding at 100 %
   */
  public BigDecimal paymentOutstanding() {
    return outstanding(EstimateSide.PAYMENT, CostType.LOSS)
        .add(outstanding(EstimateSide.PAYMENT, CostType.EXPENSE));
  }

  /**
   * Total payment estimate (loss and expense).
   *
   * @return estimate at 100 %
   */
  public BigDecimal paymentEstimate() {
    return estimateLoss.add(estimateExpense);
  }

  /**
   * Total paid (loss and expense).
   *
   * @return paid at 100 %
   */
  public BigDecimal totalPaid() {
    return paidLoss.add(paidExpense);
  }

  /**
   * Sets a new estimate.
   *
   * @param side side
   * @param cost cost type
   * @param value new estimate at 100 %
   * @throws BusinessRuleException when below the amount already paid or recovered
   */
  public void setEstimate(EstimateSide side, CostType cost, BigDecimal value) {
    requireRecoveryIsLoss(side, cost);
    if (value.signum() < 0 || value.compareTo(paid(side, cost)) < 0) {
      throw new BusinessRuleException(
          "ESTIMATE_BELOW_PAID",
          "The "
              + label(side, cost)
              + " estimate cannot be below the amount already "
              + (side == EstimateSide.PAYMENT ? "paid " : "recovered ")
              + paid(side, cost));
    }
    if (side == EstimateSide.RECOVERY) {
      estimateRecovery = value;
    } else if (cost == CostType.LOSS) {
      estimateLoss = value;
    } else {
      estimateExpense = value;
    }
  }

  /**
   * Records an amount settled or recovered, consuming the outstanding.
   *
   * @param side side
   * @param cost cost type
   * @param amount amount at 100 %
   * @throws BusinessRuleException when above the outstanding
   */
  public void addPaid(EstimateSide side, CostType cost, BigDecimal amount) {
    requireRecoveryIsLoss(side, cost);
    requireWithinOutstanding(side, cost, amount);
    if (side == EstimateSide.RECOVERY) {
      recovered = recovered.add(amount);
    } else if (cost == CostType.LOSS) {
      paidLoss = paidLoss.add(amount);
    } else {
      paidExpense = paidExpense.add(amount);
    }
  }

  /**
   * Fails when an amount exceeds the outstanding of a side and cost type.
   *
   * @param side side
   * @param cost cost type
   * @param amount amount at 100 %
   */
  public void requireWithinOutstanding(EstimateSide side, CostType cost, BigDecimal amount) {
    BigDecimal outstanding = outstanding(side, cost);
    if (amount.compareTo(outstanding) > 0) {
      throw new BusinessRuleException(
          side == EstimateSide.PAYMENT ? "SETTLEMENT_EXCEEDS_RESERVE" : "RECOVERY_EXCEEDS_ESTIMATE",
          "Amount "
              + amount
              + " exceeds the outstanding "
              + label(side, cost)
              + " estimate "
              + outstanding);
    }
  }

  /**
   * Company share of the totals: each cost type rounded on its own total.
   *
   * @param policy policy facts (share %)
   * @return company-share figures
   */
  public OurShare ourShare(ClaimPolicy policy) {
    return new OurShare(
        policy.ourShare(estimateLoss).add(policy.ourShare(estimateExpense)),
        policy.ourShare(paidLoss).add(policy.ourShare(paidExpense)),
        policy.ourShare(recovered));
  }

  private static void requireRecoveryIsLoss(EstimateSide side, CostType cost) {
    if (side == EstimateSide.RECOVERY && cost != CostType.LOSS) {
      throw new BusinessRuleException(
          "RECOVERY_IS_LOSS_ONLY", "Recovery estimates and receipts relate to the loss only");
    }
  }

  private static String label(EstimateSide side, CostType cost) {
    return side == EstimateSide.RECOVERY ? "recovery" : cost.name().toLowerCase(Locale.ROOT);
  }

  public BigDecimal getEstimateLoss() {
    return estimateLoss;
  }

  public BigDecimal getEstimateExpense() {
    return estimateExpense;
  }

  public BigDecimal getEstimateRecovery() {
    return estimateRecovery;
  }

  public BigDecimal getPaidLoss() {
    return paidLoss;
  }

  public BigDecimal getPaidExpense() {
    return paidExpense;
  }

  public BigDecimal getRecovered() {
    return recovered;
  }
}
