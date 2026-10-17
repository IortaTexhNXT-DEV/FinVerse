package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine.Production;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The incentive calculation (CMRID.005/006):
 *
 * <ul>
 *   <li>Target tiered (No Touch, Top Up): the eligible gross production of the period reaches the
 *       highest tier whose target it meets; every eligible invoice earns its gross premium times
 *       the tier's rate (percent) times its multiplier. Below the lowest target nothing is earned.
 *   <li>Fixed per policy (Motor Mania): every eligible invoice whose basic premium meets a tier's
 *       minimum earns the fixed amount of the highest such tier; policies below the minimum earn
 *       nothing.
 * </ul>
 *
 * Excluded invoices (negative amounts, erroneous bookings, CMRID.003) earn nothing and are counted
 * apart.
 */
public final class IncentiveEngine {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private IncentiveEngine() {}

  /**
   * Computes a run.
   *
   * @param calculation target tiered or fixed per policy
   * @param tiers tiers of the scheme
   * @param candidates invoices with their exclusion reason (null when eligible)
   * @return lines and totals
   */
  public static Result compute(
      Calculation calculation, List<IncentiveTier> tiers, List<Candidate> candidates) {
    List<Candidate> eligible = candidates.stream().filter(c -> c.exclusion() == null).toList();
    BigDecimal production = sum(eligible, c -> c.production().grossPremium());
    BigDecimal excluded =
        sum(
            candidates.stream().filter(c -> c.exclusion() != null).toList(),
            c -> c.production().grossPremium());
    return calculation == Calculation.TARGET_TIERED
        ? tiered(tiers, candidates, production, excluded)
        : fixed(tiers, candidates, production, excluded);
  }

  private static Result tiered(
      List<IncentiveTier> tiers,
      List<Candidate> candidates,
      BigDecimal production,
      BigDecimal excluded) {
    Optional<IncentiveTier> tier =
        tiers.stream()
            .filter(t -> t.minProduction() != null && t.ratePercent() != null)
            .filter(t -> t.minProduction().compareTo(production) <= 0)
            .max(Comparator.comparing(IncentiveTier::minProduction));
    BigDecimal factor =
        tier.map(
                t ->
                    t.ratePercent()
                        .multiply(t.multiplier() == null ? BigDecimal.ONE : t.multiplier())
                        .divide(HUNDRED))
            .orElse(BigDecimal.ZERO);
    List<LineResult> lines = new ArrayList<>();
    for (Candidate c : candidates) {
      BigDecimal amount =
          c.exclusion() == null
              ? c.production().grossPremium().multiply(factor).setScale(2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO.setScale(2);
      lines.add(new LineResult(c.production(), c.exclusion(), amount));
    }
    String applied =
        tier.map(
                t ->
                    "Target "
                        + t.minProduction()
                        + " reached: "
                        + t.ratePercent()
                        + "% x "
                        + (t.multiplier() == null ? BigDecimal.ONE : t.multiplier()))
            .orElse("No target reached");
    return new Result(lines, production, excluded, applied);
  }

  private static Result fixed(
      List<IncentiveTier> tiers,
      List<Candidate> candidates,
      BigDecimal production,
      BigDecimal excluded) {
    List<IncentiveTier> usable =
        tiers.stream()
            .filter(t -> t.minBasicPremium() != null && t.fixedAmount() != null)
            .sorted(Comparator.comparing(IncentiveTier::minBasicPremium).reversed())
            .toList();
    List<LineResult> lines = new ArrayList<>();
    int qualifying = 0;
    for (Candidate c : candidates) {
      BigDecimal amount = BigDecimal.ZERO.setScale(2);
      if (c.exclusion() == null) {
        Optional<IncentiveTier> tier =
            usable.stream()
                .filter(t -> t.minBasicPremium().compareTo(c.production().basicPremium()) <= 0)
                .findFirst();
        if (tier.isPresent()) {
          amount = tier.get().fixedAmount().setScale(2, RoundingMode.HALF_UP);
          qualifying++;
        }
      }
      lines.add(new LineResult(c.production(), c.exclusion(), amount));
    }
    return new Result(
        lines, production, excluded, qualifying + " polic(ies) above the minimum basic premium");
  }

  private static BigDecimal sum(List<Candidate> list, Function<Candidate, BigDecimal> amount) {
    return list.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * An invoice considered by a run.
   *
   * @param production its production
   * @param exclusion exclusion rule that removes it, null when eligible
   */
  public record Candidate(Production production, String exclusion) {}

  /**
   * An invoice's result.
   *
   * @param production its production
   * @param exclusion exclusion rule, null when eligible
   * @param incentive incentive earned
   */
  public record LineResult(Production production, String exclusion, BigDecimal incentive) {}

  /**
   * A computed run.
   *
   * @param lines per invoice
   * @param eligibleProduction eligible gross production
   * @param excludedAmount excluded gross production
   * @param tierApplied description of the tier applied
   */
  public record Result(
      List<LineResult> lines,
      BigDecimal eligibleProduction,
      BigDecimal excludedAmount,
      String tierApplied) {

    /** Defensive copy. */
    public Result {
      lines = List.copyOf(lines);
    }

    /**
     * The total incentive.
     *
     * @return sum of the lines
     */
    public BigDecimal incentive() {
      return lines.stream().map(LineResult::incentive).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Eligible invoices.
     *
     * @return count
     */
    public int eligibleCount() {
      return (int) lines.stream().filter(l -> l.exclusion() == null).count();
    }

    /**
     * Excluded invoices.
     *
     * @return count
     */
    public int excludedCount() {
      return lines.size() - eligibleCount();
    }
  }
}
