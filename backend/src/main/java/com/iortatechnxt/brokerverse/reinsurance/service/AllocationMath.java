package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure reinsurance allocation arithmetic (no persistence), unit tested on its own.
 *
 * <p>Sum insured split of one risk (company share) on a proportional programme:
 *
 * <ol>
 *   <li>the <b>line</b> L is the surplus retention limit, or the quota share treaty limit when
 *       there is no surplus, or unlimited when neither is set;
 *   <li>the first layer is {@code min(SI, L)}; the quota share takes QS % of it and the company
 *       retains the rest ({@code retention first, then QS %});
 *   <li>the surplus takes {@code min(lines x L, SI - first layer)};
 *   <li>anything left over is the facultative requirement.
 * </ol>
 *
 * Premium follows the sum insured: each layer receives premium x layer SI / SI; the retention takes
 * the rounding remainder so the layers always add up to the risk premium.
 */
public final class AllocationMath {

  private static final int FRACTION_SCALE = 12;

  private AllocationMath() {}

  /**
   * Splits a risk's sum insured.
   *
   * @param si company sum insured of the risk (not negative)
   * @param capacity programme capacity
   * @return split (layers add up to the sum insured)
   */
  public static SiSplit split(BigDecimal si, Capacity capacity) {
    BigDecimal sum = Money.round(si);
    BigDecimal line = capacity.line();
    BigDecimal first = line == null ? sum : sum.min(line);
    BigDecimal quotaShare = Money.round(first.multiply(capacity.quotaShareFraction()));
    BigDecimal retention = first.subtract(quotaShare);
    BigDecimal surplus = Money.zero();
    if (capacity.surplusLines() > 0 && line != null) {
      BigDecimal surplusCapacity = line.multiply(BigDecimal.valueOf(capacity.surplusLines()));
      surplus = Money.round(surplusCapacity.min(sum.subtract(first)));
    }
    BigDecimal fac = sum.subtract(first).subtract(surplus);
    return new SiSplit(retention, quotaShare, surplus, fac);
  }

  /**
   * Premium of a layer in proportion to its sum insured.
   *
   * @param premium risk premium
   * @param layerSi layer sum insured
   * @param riskSi risk sum insured
   * @return layer premium (zero when the risk has no sum insured)
   */
  public static BigDecimal proportion(BigDecimal premium, BigDecimal layerSi, BigDecimal riskSi) {
    if (riskSi.signum() == 0) {
      return Money.zero();
    }
    return Money.round(
        premium.multiply(layerSi).divide(riskSi, FRACTION_SCALE, RoundingMode.HALF_EVEN));
  }

  /**
   * Distributes a total in proportion to weights; the last part takes the rounding remainder so the
   * parts always add up to the total. Equal weights are used when all weights are zero.
   *
   * @param total amount to distribute
   * @param weights weights (not negative)
   * @return one part per weight
   */
  public static List<BigDecimal> prorate(BigDecimal total, List<BigDecimal> weights) {
    List<BigDecimal> parts = new ArrayList<>();
    if (weights.isEmpty()) {
      return parts;
    }
    BigDecimal sum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean equal = sum.signum() == 0;
    BigDecimal whole = equal ? BigDecimal.valueOf(weights.size()) : sum;
    BigDecimal allocated = Money.zero();
    for (int i = 0; i < weights.size() - 1; i++) {
      BigDecimal weight = equal ? BigDecimal.ONE : weights.get(i);
      BigDecimal part = proportion(total, weight, whole);
      parts.add(part);
      allocated = allocated.add(part);
    }
    parts.add(Money.round(total).subtract(allocated));
    return parts;
  }

  /**
   * Distributes an amount by percentages that may total less than 100 %: each part is amount x % /
   * 100, and when the percentages total exactly 100 % the last part takes the rounding remainder.
   *
   * @param amount amount
   * @param percentages shares in %
   * @return one part per percentage
   */
  public static List<BigDecimal> byPercent(BigDecimal amount, List<BigDecimal> percentages) {
    BigDecimal hundred = BigDecimal.valueOf(100);
    BigDecimal total = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(hundred) == 0) {
      return prorate(amount, percentages);
    }
    return percentages.stream().map(p -> proportion(amount, p, hundred)).toList();
  }

  /**
   * Excess of loss recovery of a layer on a cumulative net loss: the part above the priority,
   * capped at the limit.
   *
   * @param netLoss cumulative net retained loss of the claim
   * @param priority layer priority (deductible)
   * @param limit layer limit
   * @return recoverable amount (0 to limit)
   */
  public static BigDecimal excessOfLoss(BigDecimal netLoss, BigDecimal priority, BigDecimal limit) {
    BigDecimal above = netLoss.subtract(priority);
    if (above.signum() <= 0) {
      return Money.zero();
    }
    return Money.round(above.min(limit));
  }

  /**
   * Capacity of a proportional programme.
   *
   * @param quotaShareFraction quota share rate (0 to 1), zero without quota share
   * @param line retention line (see class comment), null when unlimited
   * @param surplusLines number of surplus lines, 0 without surplus
   */
  public record Capacity(BigDecimal quotaShareFraction, BigDecimal line, int surplusLines) {

    /** Capacity when no proportional treaty applies: everything is retained. */
    public static final Capacity NONE = new Capacity(BigDecimal.ZERO, null, 0);

    /**
     * The same capacity expressed in another currency (treaty limits are in the base currency).
     *
     * @param rate base currency units per unit of the other currency
     * @return capacity with the line converted
     */
    public Capacity inCurrency(BigDecimal rate) {
      if (line == null || rate.compareTo(BigDecimal.ONE) == 0) {
        return this;
      }
      return new Capacity(
          quotaShareFraction, line.divide(rate, Money.SCALE, RoundingMode.HALF_EVEN), surplusLines);
    }

    /**
     * Total sum insured the treaties can absorb per risk.
     *
     * @return line x (1 + lines), null when unlimited
     */
    public BigDecimal total() {
      return line == null ? null : line.multiply(BigDecimal.valueOf(1L + surplusLines));
    }
  }

  /**
   * Sum insured split of one risk.
   *
   * @param retention kept by the company
   * @param quotaShare ceded to the quota share
   * @param surplus ceded to the surplus
   * @param fac facultative requirement
   */
  public record SiSplit(
      BigDecimal retention, BigDecimal quotaShare, BigDecimal surplus, BigDecimal fac) {}
}
