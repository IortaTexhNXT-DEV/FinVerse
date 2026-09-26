package com.iortatechnxt.brokerverse.claims.seed;

import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Deterministic plan of the claims seed portfolio: which seed policies suffer a loss, when, how
 * large, and which lifecycle scenario the claim follows. Pure logic (no database), so the plan is
 * the same on every seed start and is unit tested.
 *
 * <p>About {@value #TARGET} claims with losses from February to July 2026, spread over the lines of
 * business, rotating through every {@link Scenario}; the first USD marine policy, the first
 * coinsurance led by the company and the first coinsurance it follows are always included.
 */
final class SeedClaimPlan {

  /** Number of claims aimed at. */
  static final int TARGET = 60;

  /** Earliest date of loss. */
  static final LocalDate FIRST_LOSS = LocalDate.of(2026, 2, 1);

  /** Latest date of loss (leaves room for the claim's later steps). */
  static final LocalDate LAST_LOSS = LocalDate.of(2026, 7, 31);

  private static final int STRIDE = 3;
  private static final int SKIPPED_SLOT = 2;
  private static final int LOSS_OFFSET_DAYS = 20;
  private static final int LOSS_SPREAD_DAYS = 60;
  private static final int LOSS_STEP = 13;
  private static final int REPORT_LAG_DAYS = 3;
  private static final int LATE_LAG_DAYS = 40;
  private static final int LATE_CYCLE = 13;
  private static final int LATE_SLOT = 5;
  private static final int RESERVE_CYCLE = 50;
  private static final int RESERVE_STEP = 37;
  private static final int RESERVE_MIN_PERMILLE = 5;
  private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
  private static final BigDecimal MINIMUM_RESERVE = BigDecimal.valueOf(25_000);
  private static final String USD = "USD";
  private static final int HUNDREDS = -2;

  /** Lifecycle followed by a seed claim. */
  enum Scenario {
    /** Reserve approved, still open. */
    OPEN,
    /** Reserve increased, partial settlement (motor: LPO and garage settlement). */
    PARTIAL,
    /** Expense paid to the surveyor, final loss settlement with deductible: closed. */
    FINAL,
    /** Reserve reduced, then final settlement: closed. */
    REDUCED_FINAL,
    /** Reserve approved, then repudiated. */
    REPUDIATED,
    /** Reserve approved, an increase waits for approval. */
    PENDING_INCREASE,
    /** Finally settled, reopened with a new reserve. */
    REOPENED,
    /** Notified without reserve, withdrawn by the claimant. */
    WITHDRAWN,
    /** Partially settled, a second settlement waits for approval. */
    PENDING_SETTLEMENT,
    /** Recovery estimate, final settlement and salvage received. */
    SALVAGE
  }

  /**
   * One planned claim.
   *
   * @param policy policy
   * @param scenario lifecycle
   * @param lossDate date of loss
   * @param reportedDate date notified
   * @param lossReserve initial loss reserve at 100 %
   */
  record PlannedClaim(
      PolicySnapshot policy,
      Scenario scenario,
      LocalDate lossDate,
      LocalDate reportedDate,
      BigDecimal lossReserve) {}

  private SeedClaimPlan() {}

  /**
   * Plans the seed claims.
   *
   * @param candidates approved seed policies, in issue order
   * @param sumsInsured sum insured at 100 % by policy id
   * @return planned claims
   */
  static List<PlannedClaim> plan(
      List<PolicySnapshot> candidates, Map<Long, BigDecimal> sumsInsured) {
    Map<Long, Scenario> chosen = choose(candidates);
    List<PlannedClaim> planned = new ArrayList<>();
    for (int i = 0; i < candidates.size(); i++) {
      PolicySnapshot p = candidates.get(i);
      Scenario s = chosen.get(p.id());
      LocalDate loss = lossDate(p, i);
      if (s != null && loss != null) {
        int lag = i % LATE_CYCLE == LATE_SLOT ? LATE_LAG_DAYS : REPORT_LAG_DAYS;
        planned.add(
            new PlannedClaim(p, s, loss, loss.plusDays(lag), reserve(sumsInsured.get(p.id()), i)));
      }
    }
    return planned;
  }

  /** Scenario of each chosen policy: the special policies first, then a rotation. */
  private static Map<Long, Scenario> choose(List<PolicySnapshot> candidates) {
    Map<Long, Scenario> chosen = new LinkedHashMap<>();
    special(candidates, p -> USD.equals(p.currency()))
        .ifPresent(p -> chosen.put(p.id(), Scenario.FINAL));
    special(candidates, p -> leads(p, true)).ifPresent(p -> chosen.put(p.id(), Scenario.PARTIAL));
    special(candidates, p -> leads(p, false)).ifPresent(p -> chosen.put(p.id(), Scenario.OPEN));
    Scenario[] rotation = Scenario.values();
    int slot = 0;
    for (int i = 0; i < candidates.size() && chosen.size() < TARGET; i++) {
      PolicySnapshot p = candidates.get(i);
      if (i % STRIDE != SKIPPED_SLOT && !chosen.containsKey(p.id()) && lossDate(p, i) != null) {
        chosen.put(p.id(), rotation[slot++ % rotation.length]);
      }
    }
    return chosen;
  }

  /**
   * Date of loss of candidate {@code i}: 20 to 80 days after issue, within February – July 2026 and
   * inside the cover.
   *
   * @param p policy
   * @param i candidate index
   * @return loss date, null when the policy is not in force on it
   */
  static LocalDate lossDate(PolicySnapshot p, int i) {
    LocalDate date =
        p.issueDate().plusDays(LOSS_OFFSET_DAYS + (long) (i * LOSS_STEP % LOSS_SPREAD_DAYS));
    if (date.isBefore(FIRST_LOSS)) {
      date = FIRST_LOSS;
    }
    return date.isAfter(LAST_LOSS) || !p.isInForce(date) ? null : date;
  }

  /**
   * Initial loss reserve: 0.5 % to 5.4 % of the sum insured, at least 25,000 (or the sum insured
   * when smaller), rounded to hundreds.
   *
   * @param sumInsured sum insured at 100 %
   * @param i candidate index
   * @return reserve at 100 %
   */
  static BigDecimal reserve(BigDecimal sumInsured, int i) {
    BigDecimal si = sumInsured == null ? MINIMUM_RESERVE : sumInsured;
    int permille = RESERVE_MIN_PERMILLE + i * RESERVE_STEP % RESERVE_CYCLE;
    BigDecimal amount =
        si.multiply(BigDecimal.valueOf(permille))
            .divide(THOUSAND, HUNDREDS, RoundingMode.HALF_EVEN);
    return amount.max(MINIMUM_RESERVE.min(si)).setScale(2, RoundingMode.HALF_EVEN);
  }

  private static boolean leads(PolicySnapshot p, boolean leader) {
    return p.businessType() == BusinessType.DIRECT_WITH_COINSURANCE
        && p.coinsuranceLeader() == leader;
  }

  private static Optional<PolicySnapshot> special(
      List<PolicySnapshot> candidates, Predicate<PolicySnapshot> test) {
    for (int i = 0; i < candidates.size(); i++) {
      if (test.test(candidates.get(i)) && lossDate(candidates.get(i), i) != null) {
        return Optional.of(candidates.get(i));
      }
    }
    return Optional.empty();
  }
}
