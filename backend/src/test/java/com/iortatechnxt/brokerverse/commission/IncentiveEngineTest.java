package com.iortatechnxt.brokerverse.commission;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine.Production;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveTier;
import com.iortatechnxt.brokerverse.commission.service.IncentiveEngine;
import com.iortatechnxt.brokerverse.commission.service.IncentiveEngine.Candidate;
import com.iortatechnxt.brokerverse.commission.service.IncentiveEngine.Result;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The incentive calculation (CMRID.003/005/006). */
class IncentiveEngineTest {

  private static Candidate candidate(String no, String basic, String gross, String exclusion) {
    return new Candidate(
        new Production(
            no, "INS", "UNIT", "CBG", "MOTOR", new BigDecimal(basic), new BigDecimal(gross)),
        exclusion);
  }

  private static IncentiveTier target(String min, String rate, String multiplier) {
    return new IncentiveTier(
        new BigDecimal(min),
        new BigDecimal(rate),
        multiplier == null ? null : new BigDecimal(multiplier),
        null,
        null);
  }

  private static IncentiveTier fixed(String minBasic, String amount) {
    return new IncentiveTier(null, null, null, new BigDecimal(minBasic), new BigDecimal(amount));
  }

  @Test
  void theHighestTargetReachedAppliesToEveryEligibleInvoice() {
    List<Candidate> candidates =
        List.of(
            candidate("A", "10000", "60000", null),
            candidate("B", "10000", "50000", null),
            candidate("C", "-500", "-600", "NEGATIVE_AMOUNT"));
    Result result =
        IncentiveEngine.compute(
            Calculation.TARGET_TIERED,
            List.of(
                target("50000", "1", null),
                target("100000", "2", "1.5"),
                target("500000", "5", null)),
            candidates);
    assertThat(result.eligibleProduction()).isEqualByComparingTo("110000");
    assertThat(result.excludedAmount()).isEqualByComparingTo("-600");
    assertThat(result.eligibleCount()).isEqualTo(2);
    assertThat(result.excludedCount()).isEqualTo(1);
    assertThat(result.tierApplied()).contains("100000");
    assertThat(result.lines().get(0).incentive()).isEqualByComparingTo("1800.00");
    assertThat(result.lines().get(1).incentive()).isEqualByComparingTo("1500.00");
    assertThat(result.lines().get(2).incentive()).isEqualByComparingTo("0");
    assertThat(result.incentive()).isEqualByComparingTo("3300.00");
  }

  @Test
  void nothingIsEarnedBelowTheLowestTarget() {
    Result result =
        IncentiveEngine.compute(
            Calculation.TARGET_TIERED,
            List.of(target("1000000", "3", "1")),
            List.of(candidate("A", "1000", "2000", null)));
    assertThat(result.incentive()).isEqualByComparingTo("0");
    assertThat(result.tierApplied()).isEqualTo("No target reached");
  }

  @Test
  void motorManiaPaysTheFixedAmountOfTheHighestMinimumMet() {
    Result result =
        IncentiveEngine.compute(
            Calculation.FIXED_PER_POLICY,
            List.of(fixed("10000", "500"), fixed("50000", "1000")),
            List.of(
                candidate("A", "60000", "70000", null),
                candidate("B", "20000", "25000", null),
                candidate("C", "5000", "6000", null),
                candidate("D", "90000", "100000", "ERRONEOUS_BOOKING")));
    assertThat(result.lines())
        .extracting(l -> l.incentive().toPlainString())
        .containsExactly("1000.00", "500.00", "0.00", "0.00");
    assertThat(result.incentive()).isEqualByComparingTo("1500.00");
    assertThat(result.tierApplied()).startsWith("2 polic");
  }
}
