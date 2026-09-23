package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimTotals;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.OurShare;
import java.math.BigDecimal;

/**
 * Estimate and paid totals of a claim, at 100 % and company share.
 *
 * @param estimateLoss loss estimate (100 %)
 * @param estimateExpense expense estimate (100 %)
 * @param estimateRecovery recovery estimate (100 %)
 * @param paidLoss loss settled (100 %)
 * @param paidExpense expenses settled (100 %)
 * @param recovered recovered (100 %)
 * @param outstandingLoss loss outstanding (100 %)
 * @param outstandingExpense expense outstanding (100 %)
 * @param recoveryOutstanding recovery still expected (100 %)
 * @param ourEstimate company share of the payment estimate
 * @param ourPaid company share settled
 * @param ourOutstanding company share outstanding (the claim reserve)
 * @param ourRecovered company share recovered
 */
public record ClaimTotalsResponse(
    BigDecimal estimateLoss,
    BigDecimal estimateExpense,
    BigDecimal estimateRecovery,
    BigDecimal paidLoss,
    BigDecimal paidExpense,
    BigDecimal recovered,
    BigDecimal outstandingLoss,
    BigDecimal outstandingExpense,
    BigDecimal recoveryOutstanding,
    BigDecimal ourEstimate,
    BigDecimal ourPaid,
    BigDecimal ourOutstanding,
    BigDecimal ourRecovered) {

  /**
   * Maps the totals of a claim.
   *
   * @param c claim
   * @return response
   */
  public static ClaimTotalsResponse from(Claim c) {
    ClaimTotals t = c.getTotals();
    OurShare our = c.ourShare();
    return new ClaimTotalsResponse(
        t.getEstimateLoss(),
        t.getEstimateExpense(),
        t.getEstimateRecovery(),
        t.getPaidLoss(),
        t.getPaidExpense(),
        t.getRecovered(),
        t.outstanding(EstimateSide.PAYMENT, CostType.LOSS),
        t.outstanding(EstimateSide.PAYMENT, CostType.EXPENSE),
        t.outstanding(EstimateSide.RECOVERY, CostType.LOSS),
        our.estimate(),
        our.paid(),
        our.outstanding(),
        our.recovered());
  }
}
