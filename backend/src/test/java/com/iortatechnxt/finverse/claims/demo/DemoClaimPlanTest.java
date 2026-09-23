package com.iortatechnxt.finverse.claims.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.claims.demo.DemoClaimPlan.PlannedClaim;
import com.iortatechnxt.finverse.claims.demo.DemoClaimPlan.Scenario;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DemoClaimPlanTest {

  private static final String[] LINES = {"FIRE", "MOTOR", "MARINE", "ENGG", "CASUALTY", "PA"};

  private static PolicySnapshot policy(long id, LocalDate issue, String currency, int coins) {
    boolean coinsured = coins > 0;
    return new PolicySnapshot(
        id,
        "P-" + id,
        1L,
        1L,
        1L,
        "PROD",
        "Product",
        LINES[(int) (id % LINES.length)],
        "C-000201",
        "Client",
        "Insured",
        SourceType.DIRECT,
        null,
        null,
        issue,
        issue,
        issue.plusYears(1).minusDays(1),
        2026,
        currency,
        coinsured ? BusinessType.DIRECT_WITH_COINSURANCE : BusinessType.DIRECT,
        new BigDecimal(coinsured ? "60" : "100"),
        coinsured ? "CO-0001" : null,
        coins == 1,
        PolicyStatus.APPROVED,
        issue,
        null,
        null);
  }

  @Test
  void planCoversEveryScenarioAndTheSpecialPolicies() {
    List<PolicySnapshot> candidates = new ArrayList<>();
    Map<Long, BigDecimal> sums = new HashMap<>();
    for (long i = 0; i < 150; i++) {
      LocalDate issue = LocalDate.of(2026, 1, 5).plusDays(i * 256 / 150);
      String currency = i == 40 ? "USD" : "PHP";
      int coins = i == 20 ? 1 : i == 22 ? 2 : 0;
      candidates.add(policy(i, issue, currency, coins));
      sums.put(i, BigDecimal.valueOf(1_000_000 + i * 10_000));
    }

    List<PlannedClaim> plan = DemoClaimPlan.plan(candidates, sums);

    assertThat(plan).hasSize(DemoClaimPlan.TARGET);
    assertThat(plan.stream().map(PlannedClaim::scenario).collect(Collectors.toSet()))
        .isEqualTo(EnumSet.allOf(Scenario.class));
    assertThat(plan)
        .anyMatch(p -> "USD".equals(p.policy().currency()) && p.scenario() == Scenario.FINAL);
    assertThat(plan)
        .anyMatch(p -> p.policy().coinsuranceLeader() && p.scenario() == Scenario.PARTIAL);
    assertThat(plan)
        .allMatch(p -> !p.lossDate().isBefore(DemoClaimPlan.FIRST_LOSS))
        .allMatch(p -> !p.lossDate().isAfter(DemoClaimPlan.LAST_LOSS))
        .allMatch(p -> !p.reportedDate().isBefore(p.lossDate()))
        .allMatch(p -> p.lossReserve().signum() > 0);
    assertThat(plan).anyMatch(p -> p.reportedDate().minusDays(30).isAfter(p.lossDate()));
    assertThat(DemoClaimPlan.plan(candidates, sums)).isEqualTo(plan);
  }

  @Test
  void reserveAndLossDateRules() {
    assertThat(DemoClaimPlan.reserve(new BigDecimal("1000000"), 0)).isEqualByComparingTo("25000");
    assertThat(DemoClaimPlan.reserve(new BigDecimal("10000"), 0)).isEqualByComparingTo("10000");
    assertThat(DemoClaimPlan.reserve(null, 3)).isEqualByComparingTo("25000");
    assertThat(DemoClaimPlan.reserve(new BigDecimal("100000000"), 1))
        .isEqualByComparingTo("4200000");
    PolicySnapshot late = policy(1, LocalDate.of(2026, 9, 1), "PHP", 0);
    assertThat(DemoClaimPlan.lossDate(late, 0)).isNull();
    PolicySnapshot early = policy(2, LocalDate.of(2025, 12, 1), "PHP", 0);
    assertThat(DemoClaimPlan.lossDate(early, 0)).isEqualTo(DemoClaimPlan.FIRST_LOSS);
    assertThat(DemoLoss.of("UNKNOWN").nature()).isEqualTo("Accidental damage");
    assertThat(DemoLoss.of("MOTOR").nature()).isEqualTo("Collision");
  }
}
