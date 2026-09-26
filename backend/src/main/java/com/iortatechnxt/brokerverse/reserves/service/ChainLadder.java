package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic chain-ladder projection of a cumulative development triangle.
 *
 * <ul>
 *   <li>Age-to-age factor f(k) = Σ C(i, k+1) / Σ C(i, k) over the accident periods i observed at
 *       both ages (volume weighted); a zero denominator gives a factor of 1.
 *   <li>Cumulative development factor of accident period i = product of the factors from its latest
 *       age to the last age of the triangle (no tail factor beyond the oldest period).
 *   <li>Ultimate(i) = latest C(i) × cumulative factor.
 * </ul>
 */
public final class ChainLadder {

  /** Scale of development factors. */
  public static final int FACTOR_SCALE = 6;

  private ChainLadder() {}

  /**
   * Projects a triangle to ultimate.
   *
   * @param triangle cumulative triangle
   * @return factors and ultimates
   */
  public static Projection project(Triangle triangle) {
    int columns = triangle.developmentPeriods();
    List<BigDecimal> factors = new ArrayList<>();
    for (int k = 0; k < columns - 1; k++) {
      factors.add(factor(triangle, k));
    }
    List<BigDecimal> cumulative = new ArrayList<>();
    List<BigDecimal> ultimates = new ArrayList<>();
    for (int i = 0; i < triangle.rows().size(); i++) {
      int age = Math.max(triangle.rows().get(i).size() - 1, 0);
      BigDecimal cdf = BigDecimal.ONE;
      for (int k = age; k < factors.size(); k++) {
        cdf = cdf.multiply(factors.get(k));
      }
      cdf = cdf.setScale(FACTOR_SCALE, RoundingMode.HALF_EVEN);
      cumulative.add(cdf);
      ultimates.add(Money.round(triangle.latest(i).multiply(cdf)));
    }
    return new Projection(factors, cumulative, ultimates);
  }

  private static BigDecimal factor(Triangle triangle, int age) {
    BigDecimal numerator = BigDecimal.ZERO;
    BigDecimal denominator = BigDecimal.ZERO;
    for (List<BigDecimal> row : triangle.rows()) {
      if (row.size() > age + 1) {
        numerator = numerator.add(row.get(age + 1));
        denominator = denominator.add(row.get(age));
      }
    }
    return denominator.signum() == 0
        ? BigDecimal.ONE.setScale(FACTOR_SCALE, RoundingMode.HALF_EVEN)
        : numerator.divide(denominator, FACTOR_SCALE, RoundingMode.HALF_EVEN);
  }

  /**
   * Result of a projection.
   *
   * @param factors age-to-age factors f(0), f(1), ...
   * @param cumulativeFactors cumulative development factor of each accident period
   * @param ultimates projected ultimate of each accident period
   */
  public record Projection(
      List<BigDecimal> factors, List<BigDecimal> cumulativeFactors, List<BigDecimal> ultimates) {

    /** Canonical constructor copying the lists. */
    public Projection {
      factors = List.copyOf(factors);
      cumulativeFactors = List.copyOf(cumulativeFactors);
      ultimates = List.copyOf(ultimates);
    }
  }
}
