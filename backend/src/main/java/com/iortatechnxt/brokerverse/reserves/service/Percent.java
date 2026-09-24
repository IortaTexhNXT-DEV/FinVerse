package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Percentage and allocation arithmetic shared by the reserve calculations (money at 2 dp). */
public final class Percent {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int RATIO_SCALE = 10;

  private Percent() {}

  /**
   * Amount × percentage.
   *
   * @param amount amount
   * @param pct percentage (12.5 = 12.5 %), null = 0
   * @return rounded amount
   */
  public static BigDecimal of(BigDecimal amount, BigDecimal pct) {
    return Money.round(
        amount.multiply(Money.nz(pct)).divide(HUNDRED, RATIO_SCALE, RoundingMode.HALF_EVEN));
  }

  /**
   * Ratio of two amounts.
   *
   * @param part numerator
   * @param whole denominator
   * @return part / whole, zero when the denominator is not positive
   */
  public static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
    if (whole.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return part.divide(whole, RATIO_SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Splits an amount over keys in proportion to non-negative weights. The rounding difference goes
   * to the key with the largest weight, so the parts always add up to the amount.
   *
   * @param amount amount to split
   * @param weights weight by key
   * @param <K> key type
   * @return part by key (empty when no weight is positive)
   */
  public static <K> Map<K, BigDecimal> allocate(BigDecimal amount, Map<K, BigDecimal> weights) {
    List<Map.Entry<K, BigDecimal>> positive = new ArrayList<>();
    weights.entrySet().stream().filter(e -> e.getValue().signum() > 0).forEach(positive::add);
    Map<K, BigDecimal> out = new LinkedHashMap<>();
    if (positive.isEmpty()) {
      return out;
    }
    BigDecimal total =
        positive.stream().map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal allocated = BigDecimal.ZERO;
    for (Map.Entry<K, BigDecimal> e : positive) {
      BigDecimal part = Money.round(amount.multiply(ratio(e.getValue(), total)));
      out.put(e.getKey(), part);
      allocated = allocated.add(part);
    }
    K largest =
        positive.stream().max(Comparator.comparing(Map.Entry::getValue)).orElseThrow().getKey();
    out.merge(largest, Money.round(amount).subtract(allocated), BigDecimal::add);
    return out;
  }
}
