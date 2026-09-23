package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.MovementKind;
import com.iortatechnxt.finverse.claims.domain.MovementTotal;
import com.iortatechnxt.finverse.common.util.Money;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Estimate and paid figures of one claim over a period, company share, built from the movement
 * ledger (claim currency or base currency). Payment estimate = Reports Book types 1 + 3, recovery
 * estimate = types 2 + 4; outstanding = estimate − paid.
 *
 * @param estimateLoss payment estimate of the loss
 * @param estimateExpense payment estimate of expenses
 * @param estimateRecovery recovery estimate
 * @param paidLoss loss settled
 * @param paidExpense expenses settled
 * @param recovered amount recovered
 */
public record ClaimFigures(
    BigDecimal estimateLoss,
    BigDecimal estimateExpense,
    BigDecimal estimateRecovery,
    BigDecimal paidLoss,
    BigDecimal paidExpense,
    BigDecimal recovered) {

  /**
   * Figures of a claim without movements.
   *
   * @return zeros
   */
  public static ClaimFigures none() {
    BigDecimal z = Money.zero();
    return new ClaimFigures(z, z, z, z, z, z);
  }

  /**
   * Sums movement totals of one claim.
   *
   * @param totals totals of the claim
   * @param base true for base-currency amounts, false for claim-currency amounts
   * @return figures
   */
  public static ClaimFigures of(Collection<MovementTotal> totals, boolean base) {
    Map<Bucket, BigDecimal> sums = new EnumMap<>(Bucket.class);
    for (MovementTotal t : totals) {
      sums.merge(Bucket.of(t), base ? t.baseAmount() : t.amount(), BigDecimal::add);
    }
    return new ClaimFigures(
        sum(sums, Bucket.ESTIMATE_LOSS),
        sum(sums, Bucket.ESTIMATE_EXPENSE),
        sum(sums, Bucket.ESTIMATE_RECOVERY),
        sum(sums, Bucket.PAID_LOSS),
        sum(sums, Bucket.PAID_EXPENSE),
        sum(sums, Bucket.RECOVERED));
  }

  private static BigDecimal sum(Map<Bucket, BigDecimal> sums, Bucket bucket) {
    return sums.getOrDefault(bucket, Money.zero());
  }

  /**
   * Adds the figures of another claim (policy or product totals).
   *
   * @param o other figures
   * @return sum
   */
  public ClaimFigures plus(ClaimFigures o) {
    return new ClaimFigures(
        estimateLoss.add(o.estimateLoss),
        estimateExpense.add(o.estimateExpense),
        estimateRecovery.add(o.estimateRecovery),
        paidLoss.add(o.paidLoss),
        paidExpense.add(o.paidExpense),
        recovered.add(o.recovered));
  }

  /**
   * Amount settled net of recoveries (loss and expense).
   *
   * @return paid − recovered
   */
  public BigDecimal netPaid() {
    return paid(true).subtract(recovered);
  }

  /**
   * Payment estimate.
   *
   * @param includeExpense whether the expense provision is included
   * @return estimate
   */
  public BigDecimal paymentEstimate(boolean includeExpense) {
    return includeExpense ? estimateLoss.add(estimateExpense) : estimateLoss;
  }

  /**
   * Amount settled.
   *
   * @param includeExpense whether expenses are included
   * @return paid
   */
  public BigDecimal paid(boolean includeExpense) {
    return includeExpense ? paidLoss.add(paidExpense) : paidLoss;
  }

  /**
   * Payment outstanding (estimate − paid).
   *
   * @param includeExpense whether the expense provision is included
   * @return outstanding
   */
  public BigDecimal paymentOutstanding(boolean includeExpense) {
    return paymentEstimate(includeExpense).subtract(paid(includeExpense));
  }

  /**
   * Recovery outstanding (recovery estimate − recovered).
   *
   * @return outstanding
   */
  public BigDecimal recoveryOutstanding() {
    return estimateRecovery.subtract(recovered);
  }

  /**
   * Whether every figure is zero.
   *
   * @return true when the claim had no movement
   */
  public boolean isEmpty() {
    return paymentEstimate(true).signum() == 0
        && paid(true).signum() == 0
        && estimateRecovery.signum() == 0
        && recovered.signum() == 0;
  }

  /** Figure a movement total adds to. */
  private enum Bucket {
    ESTIMATE_LOSS,
    ESTIMATE_EXPENSE,
    ESTIMATE_RECOVERY,
    PAID_LOSS,
    PAID_EXPENSE,
    RECOVERED;

    static Bucket of(MovementTotal t) {
      boolean estimate = t.kind() == MovementKind.ESTIMATE;
      if (t.side() == EstimateSide.RECOVERY) {
        return estimate ? ESTIMATE_RECOVERY : RECOVERED;
      }
      if (t.costType() == CostType.LOSS) {
        return estimate ? ESTIMATE_LOSS : PAID_LOSS;
      }
      return estimate ? ESTIMATE_EXPENSE : PAID_EXPENSE;
    }
  }
}
