package com.iortatechnxt.brokerverse.budget.service;

import com.iortatechnxt.brokerverse.budget.domain.BudgetLine;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Budget allocation arithmetic: spreading an annual amount over twelve months and scaling.
 *
 * <p>Every spread returns amounts at monetary scale whose sum equals the annual amount exactly; the
 * rounding remainder is placed in the last month.
 */
public final class BudgetSpread {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int WORK_SCALE = 10;

  private BudgetSpread() {}

  /**
   * Spreads an annual amount evenly.
   *
   * @param annual annual amount
   * @return twelve monthly amounts
   */
  public static List<BigDecimal> even(BigDecimal annual) {
    return weighted(annual, Collections.nCopies(BudgetLine.MONTHS, BigDecimal.ONE));
  }

  /**
   * Spreads an annual amount in proportion to monthly weights (seasonality).
   *
   * @param annual annual amount
   * @param weights twelve non-negative weights, not all zero
   * @return twelve monthly amounts
   */
  public static List<BigDecimal> weighted(BigDecimal annual, List<BigDecimal> weights) {
    if (weights.size() != BudgetLine.MONTHS) {
      throw new BusinessRuleException(
          "INVALID_SEASONALITY", "Seasonality needs exactly twelve monthly weights");
    }
    BigDecimal totalWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (totalWeight.signum() <= 0 || weights.stream().anyMatch(w -> w.signum() < 0)) {
      throw new BusinessRuleException(
          "INVALID_SEASONALITY", "Seasonality weights must be positive");
    }
    BigDecimal target = Money.round(annual);
    List<BigDecimal> months = new ArrayList<>(BudgetLine.MONTHS);
    BigDecimal allocated = Money.zero();
    for (int i = 0; i < BudgetLine.MONTHS - 1; i++) {
      BigDecimal share =
          Money.round(
              target
                  .multiply(weights.get(i))
                  .divide(totalWeight, WORK_SCALE, RoundingMode.HALF_EVEN));
      months.add(share);
      allocated = allocated.add(share);
    }
    months.add(target.subtract(allocated));
    return months;
  }

  /**
   * Scales monthly amounts by a percentage (e.g. prior-year actuals plus 5 %).
   *
   * @param months monthly amounts
   * @param percent adjustment in percent (negative reduces)
   * @return scaled amounts
   */
  public static List<BigDecimal> scale(List<BigDecimal> months, BigDecimal percent) {
    BigDecimal factor =
        BigDecimal.ONE.add(percent.divide(HUNDRED, WORK_SCALE, RoundingMode.HALF_EVEN));
    return months.stream().map(m -> Money.round(m.multiply(factor))).toList();
  }
}
