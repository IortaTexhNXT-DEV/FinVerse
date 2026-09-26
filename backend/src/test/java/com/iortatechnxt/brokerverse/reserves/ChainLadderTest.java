package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.brokerverse.reserves.service.ChainLadder;
import com.iortatechnxt.brokerverse.reserves.service.ClaimsTriangleBuilder;
import com.iortatechnxt.brokerverse.reserves.service.Triangle;
import com.iortatechnxt.brokerverse.reserves.service.TriangleAnalysis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Chain-ladder projection (worked example with known factors) and triangle building. */
class ChainLadderTest {

  private static List<BigDecimal> row(String... values) {
    return Arrays.stream(values).map(BigDecimal::new).toList();
  }

  /**
   * Worked example: AY1 100, 150, 175; AY2 110, 168; AY3 120. f(0) = (150 + 168) / (100 + 110) =
   * 1.514286, f(1) = 175 / 150 = 1.166667. Ultimates: AY1 175, AY2 196, AY3 120 x 1.766668 = 212.
   */
  @Test
  void workedExampleProjectsTheKnownUltimates() {
    Triangle t =
        new Triangle(
            List.of("2024", "2025", "2026"),
            List.of(row("100", "150", "175"), row("110", "168"), row("120")));
    ChainLadder.Projection p = ChainLadder.project(t);
    assertThat(p.factors()).containsExactly(new BigDecimal("1.514286"), new BigDecimal("1.166667"));
    assertThat(p.cumulativeFactors().get(0)).isEqualByComparingTo("1");
    assertThat(p.cumulativeFactors().get(2)).isEqualByComparingTo("1.766668");
    assertThat(p.ultimates())
        .extracting(BigDecimal::toPlainString)
        .containsExactly("175.00", "196.00", "212.00");
  }

  @Test
  void zeroDenominatorGivesAFactorOfOne() {
    Triangle t = new Triangle(List.of("A", "B"), List.of(row("0", "50"), row("80")));
    assertThat(ChainLadder.project(t).factors().get(0)).isEqualByComparingTo("1");
  }

  private static ClaimMovement movement(
      String loss, String at, ClaimMovementType type, String amount) {
    return new ClaimMovement(
        1L,
        1L,
        9L,
        "CL-1",
        7L,
        "FIRE",
        LocalDate.parse(loss),
        LocalDate.parse(at),
        type,
        "PHP",
        new BigDecimal(amount),
        new BigDecimal(amount),
        "REF-" + at + type);
  }

  @Test
  void movementsBuildPaidAndIncurredTriangles() {
    List<ClaimMovement> movements =
        List.of(
            movement("2025-03-01", "2025-03-10", ClaimMovementType.RESERVE_CHANGE, "100"),
            movement("2025-03-01", "2026-02-01", ClaimMovementType.PAYMENT, "60"),
            movement("2025-03-01", "2026-02-01", ClaimMovementType.RESERVE_CHANGE, "-60"),
            movement("2025-03-01", "2026-04-01", ClaimMovementType.RECOVERY, "10"),
            movement("2026-05-01", "2026-05-02", ClaimMovementType.RESERVE_CHANGE, "80"),
            // outside the triangle: after the valuation date and before the first accident year
            movement("2026-05-01", "2026-12-01", ClaimMovementType.PAYMENT, "999"),
            movement("2019-05-01", "2026-05-02", ClaimMovementType.PAYMENT, "999"));
    ClaimsTriangleBuilder.Triangles t =
        ClaimsTriangleBuilder.build(
            movements, DevelopmentPeriod.YEAR, LocalDate.of(2026, 6, 30), 2);
    assertThat(t.paid().accidentLabels()).containsExactly("2025", "2026");
    assertThat(t.paid().rows().get(0)).extracting(BigDecimal::intValue).containsExactly(0, 50);
    assertThat(t.incurred().rows().get(0))
        .extracting(BigDecimal::intValue)
        .containsExactly(100, 90);
    assertThat(t.incurred().rows().get(1)).extracting(BigDecimal::intValue).containsExactly(80);
  }

  @Test
  void analysisGivesIbnrAsUltimateLessIncurred() {
    List<ClaimMovement> movements =
        List.of(
            movement("2024-06-01", "2024-06-10", ClaimMovementType.RESERVE_CHANGE, "100"),
            movement("2024-06-01", "2025-06-10", ClaimMovementType.RESERVE_CHANGE, "50"),
            movement("2025-06-01", "2025-06-10", ClaimMovementType.RESERVE_CHANGE, "200"));
    TriangleAnalysis a =
        TriangleAnalysis.of(
            "FIRE",
            movements,
            TriangleBasis.INCURRED,
            DevelopmentPeriod.YEAR,
            2,
            LocalDate.of(2025, 12, 31));
    // f(0) = 150 / 100 = 1.5 (AY2024 only); AY2025 ultimate 200 x 1.5 = 300, IBNR 100
    assertThat(a.projection().factors().get(0)).isEqualByComparingTo("1.5");
    assertThat(a.ibnrByAccident().get(1)).isEqualByComparingTo("100");
    assertThat(a.ibnr()).isEqualByComparingTo("100");
    assertThat(a.period()).isEqualTo(DevelopmentPeriod.YEAR);
  }

  @Test
  void negativeTotalIbnrIsFloored() {
    List<ClaimMovement> movements =
        List.of(
            movement("2025-01-10", "2025-01-10", ClaimMovementType.RESERVE_CHANGE, "100"),
            movement("2025-01-10", "2025-04-10", ClaimMovementType.RESERVE_CHANGE, "-50"),
            movement("2025-04-10", "2025-04-10", ClaimMovementType.RESERVE_CHANGE, "100"));
    TriangleAnalysis a =
        TriangleAnalysis.of(
            "MOTOR",
            movements,
            TriangleBasis.PAID,
            DevelopmentPeriod.QUARTER,
            2,
            LocalDate.of(2025, 6, 30));
    assertThat(a.paid().accidentLabels()).containsExactly("2025-Q1", "2025-Q2");
    assertThat(a.ibnr()).isEqualByComparingTo("0");
    assertThat(a.ibnrByAccident().get(0)).isNegative();
  }
}
