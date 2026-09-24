package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.insurance.ClaimReinsuranceView;
import com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.brokerverse.insurance.OutstandingClaim;
import com.iortatechnxt.brokerverse.reserves.domain.DevelopmentPeriod;
import com.iortatechnxt.brokerverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.brokerverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey.PostingKey;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.domain.TriangleBasis;
import com.iortatechnxt.brokerverse.reserves.service.GrossRi;
import com.iortatechnxt.brokerverse.reserves.service.IbnrCalculator;
import com.iortatechnxt.brokerverse.reserves.service.Percent;
import com.iortatechnxt.brokerverse.reserves.service.ReserveEvent;
import com.iortatechnxt.brokerverse.reserves.service.ReserveParameterSet;
import com.iortatechnxt.brokerverse.reserves.service.ReservePorts;
import com.iortatechnxt.brokerverse.reserves.service.ReservePosting;
import com.iortatechnxt.brokerverse.reserves.service.ReserveSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** IBNR (rate and chain-ladder allocation), optional kernel ports, movements and summaries. */
class IbnrAndPostingTest {

  private static final LocalDate DATE = LocalDate.of(2026, 6, 30);
  private static final ReserveKey HO_FIRE = new ReserveKey(1L, "FIRE", "FIRE-COM", "DIRECT");
  private static final ReserveKey CEB_FIRE = new ReserveKey(2L, "FIRE", "FIRE-COM", "BROKER");
  private static final ReserveKey HO_PA = new ReserveKey(1L, "PA", "PA-IND", "AGENT");

  private static ReservePorts ports(Object... beans) {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    for (int i = 0; i < beans.length; i++) {
      factory.addBean("bean" + i, beans[i]);
    }
    return new ReservePorts(
        factory.getBeanProvider(ClaimsExperienceView.class),
        factory.getBeanProvider(ClaimReinsuranceView.class));
  }

  private static ReserveParameterTerms terms(IbnrMethod method, String rate) {
    return new ReserveParameterTerms(
        method,
        new BigDecimal(rate),
        TriangleBasis.INCURRED,
        DevelopmentPeriod.YEAR,
        2,
        new BigDecimal("10"),
        new BigDecimal("5"),
        new BigDecimal("60"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null);
  }

  private static PremiumAmounts earned(String premium, String ceded) {
    return new PremiumAmounts(
        new BigDecimal(premium),
        BigDecimal.ZERO,
        new BigDecimal(ceded),
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }

  @Test
  void rateMethodAppliesTheRateToEarnedPremiumAndTheCededShare() {
    IbnrCalculator calculator = new IbnrCalculator(ports());
    ReserveParameterSet params = new ReserveParameterSet(Map.of("PA", terms(IbnrMethod.RATE, "5")));
    IbnrCalculator.Result result =
        calculator.calculate(1L, DATE, params, Map.of(HO_PA, earned("200000", "50000")), List.of());
    ReserveLineValues line = result.lines().get(0);
    // IBNR = 200 000 x 5 % = 10 000; ceded share 25 % -> RI portion 2 500
    assertThat(line.gross()).isEqualByComparingTo("10000.00");
    assertThat(line.ri()).isEqualByComparingTo("2500.00");
    assertThat(line.base()).isEqualByComparingTo("200000.00");
    assertThat(line.method()).isEqualTo("RATE");
    assertThat(result.analyses()).isEmpty();
  }

  @Test
  void rateMethodIgnoresNegativeEarnedPremium() {
    IbnrCalculator calculator = new IbnrCalculator(ports());
    ReserveParameterSet params = new ReserveParameterSet(Map.of());
    IbnrCalculator.Result result =
        calculator.calculate(1L, DATE, params, Map.of(HO_PA, earned("-500", "0")), List.of());
    assertThat(result.lines().get(0).gross()).isEqualByComparingTo("0");
    assertThat(params.isConfigured("PA")).isFalse();
  }

  private static ClaimMovement reserve(String loss, String at, String amount) {
    return new ClaimMovement(
        1L,
        1L,
        5L,
        "CL-5",
        7L,
        "FIRE",
        LocalDate.parse(loss),
        LocalDate.parse(at),
        ClaimMovementType.RESERVE_CHANGE,
        "PHP",
        new BigDecimal(amount),
        new BigDecimal(amount),
        "R-" + at);
  }

  @Test
  void chainLadderIbnrIsAllocatedByEarnedPremium() {
    ClaimsExperienceView claims =
        new ClaimsExperienceView() {
          @Override
          public List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf) {
            return List.of();
          }

          @Override
          public List<ClaimMovement> movements(Long companyId, LocalDate from, LocalDate to) {
            return List.of(
                reserve("2025-02-01", "2025-02-01", "100"),
                reserve("2025-02-01", "2026-02-01", "50"),
                reserve("2026-03-01", "2026-03-01", "200"));
          }
        };
    IbnrCalculator calculator = new IbnrCalculator(ports(claims));
    ReserveParameterSet params =
        new ReserveParameterSet(Map.of("FIRE", terms(IbnrMethod.CHAIN_LADDER, "0")));
    Map<ReserveKey, PremiumAmounts> earned = new LinkedHashMap<>();
    earned.put(HO_FIRE, earned("300", "0"));
    earned.put(CEB_FIRE, earned("100", "50"));
    IbnrCalculator.Result result = calculator.calculate(1L, DATE, params, earned, List.of());
    // f(0) = 1.5 -> AY2026 ultimate 300, IBNR 100, split 75 / 25; CEB ceded share 50 %
    assertThat(result.analyses()).hasSize(1);
    Map<ReserveKey, ReserveLineValues> byKey = new LinkedHashMap<>();
    result.lines().forEach(l -> byKey.put(l.key(), l));
    assertThat(byKey.get(HO_FIRE).gross()).isEqualByComparingTo("75.00");
    assertThat(byKey.get(CEB_FIRE).gross()).isEqualByComparingTo("25.00");
    assertThat(byKey.get(CEB_FIRE).ri()).isEqualByComparingTo("12.50");
  }

  @Test
  void chainLadderFallsBackToClaimUnitsWithoutPremium() {
    IbnrCalculator calculator =
        new IbnrCalculator(
            ports(
                new ClaimsExperienceView() {
                  @Override
                  public List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf) {
                    return List.of();
                  }

                  @Override
                  public List<ClaimMovement> movements(
                      Long companyId, LocalDate from, LocalDate to) {
                    return List.of(
                        reserve("2025-02-01", "2025-02-01", "100"),
                        reserve("2025-02-01", "2026-02-01", "50"),
                        reserve("2026-03-01", "2026-03-01", "200"));
                  }
                }));
    ReserveParameterSet params =
        new ReserveParameterSet(Map.of("FIRE", terms(IbnrMethod.CHAIN_LADDER, "0")));
    IbnrCalculator.Result result =
        calculator.calculate(1L, DATE, params, Map.of(), List.of(HO_FIRE, CEB_FIRE));
    assertThat(result.lines()).hasSize(2);
    assertThat(result.lines().stream().map(ReserveLineValues::gross).reduce(BigDecimal::add))
        .contains(new BigDecimal("100.00"));
  }

  @Test
  void absentPortsGiveEmptyClaimsData() {
    ReservePorts none = ports();
    assertThat(none.hasClaims()).isFalse();
    assertThat(none.hasClaimReinsurance()).isFalse();
    assertThat(none.outstanding(1L, DATE)).isEmpty();
    assertThat(none.movements(1L, DATE, DATE)).isEmpty();
    assertThat(none.reinsuranceShare(1L, DATE)).isEmpty();
    ClaimReinsuranceView ri = (companyId, asOf) -> Map.of(5L, BigDecimal.TEN);
    ReservePorts some = ports(ri);
    assertThat(some.hasClaimReinsurance()).isTrue();
    assertThat(some.reinsuranceShare(1L, DATE)).containsEntry(5L, BigDecimal.TEN);
  }

  @Test
  void movementsAreClosingLessOpeningPerBranchAndLine() {
    List<ReserveLineValues> closing =
        List.of(
            ReserveLineValues.of(
                ReserveType.UPR, HO_FIRE, new BigDecimal("1000"), new BigDecimal("300")),
            ReserveLineValues.of(
                ReserveType.DAC, HO_FIRE, new BigDecimal("150"), new BigDecimal("80")),
            ReserveLineValues.of(ReserveType.OSLR, HO_FIRE, new BigDecimal("999"), BigDecimal.ZERO),
            ReserveLineValues.of(
                ReserveType.MFAD, HO_FIRE, new BigDecimal("40"), new BigDecimal("4")));
    List<ReserveLineValues> opening =
        List.of(
            ReserveLineValues.of(
                ReserveType.UPR, HO_FIRE, new BigDecimal("1200"), new BigDecimal("300")),
            ReserveLineValues.of(
                ReserveType.IBNR, HO_PA, new BigDecimal("70"), new BigDecimal("7")));
    Map<PostingKey, Map<ReserveEvent, Map<String, BigDecimal>>> m =
        ReservePosting.movements(closing, opening);
    Map<ReserveEvent, Map<String, BigDecimal>> fire = m.get(HO_FIRE.postingKey());
    assertThat(fire.get(ReserveEvent.UPR_PROVISION))
        .containsOnlyKeys("UPR_CHANGE")
        .containsEntry("UPR_CHANGE", new BigDecimal("-200.00"));
    assertThat(fire.get(ReserveEvent.DAC_PROVISION))
        .containsEntry("DRC_CHANGE", new BigDecimal("80.00"));
    assertThat(fire.get(ReserveEvent.CLAIM_MARGIN_PROVISION))
        .containsEntry("MFAD_CHANGE", new BigDecimal("40.00"))
        .containsEntry("RI_MFAD_CHANGE", new BigDecimal("4.00"));
    assertThat(fire).doesNotContainKey(ReserveEvent.IBNR_PROVISION);
    // a line that disappeared is fully released
    assertThat(m.get(HO_PA.postingKey()).get(ReserveEvent.IBNR_PROVISION))
        .containsEntry("IBNR_CHANGE", new BigDecimal("-70.00"))
        .containsEntry("RI_IBNR_CHANGE", new BigDecimal("-7.00"));
  }

  @Test
  void summarySplitsDacAndUcrAndComparesValuations() {
    List<ReserveSummary.Row> rows =
        ReserveSummary.rows(
            List.of(
                ReserveLineValues.of(
                    ReserveType.DAC, HO_FIRE, new BigDecimal("150"), new BigDecimal("80")),
                ReserveLineValues.of(
                    ReserveType.UPR, CEB_FIRE, new BigDecimal("500"), BigDecimal.ONE)),
            List.of(
                ReserveLineValues.of(
                    ReserveType.UPR, HO_FIRE, new BigDecimal("400"), BigDecimal.ZERO)));
    assertThat(rows).extracting(ReserveSummary.Row::reserve).containsExactly("UPR", "DAC", "UCR");
    assertThat(rows.get(0).netChange()).isEqualByComparingTo("99");
    assertThat(rows.get(2).current().net()).isEqualByComparingTo("-80");
  }

  @Test
  void allocationAddsUpAndPercentagesRound() {
    Map<String, BigDecimal> parts =
        Percent.allocate(
            new BigDecimal("100.00"),
            Map.of(
                "a",
                BigDecimal.ONE,
                "b",
                BigDecimal.ONE,
                "c",
                BigDecimal.ONE,
                "d",
                BigDecimal.ZERO));
    assertThat(parts).doesNotContainKey("d");
    assertThat(parts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("100.00");
    assertThat(Percent.allocate(BigDecimal.TEN, Map.of("x", BigDecimal.ZERO))).isEmpty();
    assertThat(Percent.of(new BigDecimal("333.33"), new BigDecimal("7.5")))
        .isEqualByComparingTo("25.00");
    assertThat(Percent.ratio(BigDecimal.ONE, BigDecimal.ZERO)).isZero();
    assertThat(new GrossRi(BigDecimal.TEN, BigDecimal.ONE).percent(BigDecimal.TEN).net())
        .isEqualByComparingTo("0.90");
  }
}
