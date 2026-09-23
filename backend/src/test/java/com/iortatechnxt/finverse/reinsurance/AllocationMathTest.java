package com.iortatechnxt.finverse.reinsurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.finverse.reinsurance.domain.SoaPeriod;
import com.iortatechnxt.finverse.reinsurance.service.AllocationMath;
import com.iortatechnxt.finverse.reinsurance.service.AllocationMath.Capacity;
import com.iortatechnxt.finverse.reinsurance.service.AllocationMath.SiSplit;
import com.iortatechnxt.finverse.reinsurance.service.SoaLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure allocation, excess of loss and statement arithmetic. */
class AllocationMathTest {

  private static final BigDecimal FORTY = new BigDecimal("0.40");
  private static final BigDecimal LINE = new BigDecimal("25000000");

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  @Test
  void quotaShareAndSurplusLinesWithFacultativeRemainder() {
    SiSplit s = AllocationMath.split(bd("120000000"), new Capacity(FORTY, LINE, 3));

    assertThat(s.quotaShare()).isEqualByComparingTo("10000000");
    assertThat(s.retention()).isEqualByComparingTo("15000000");
    assertThat(s.surplus()).isEqualByComparingTo("75000000");
    assertThat(s.fac()).isEqualByComparingTo("20000000");
  }

  @Test
  void riskWithinTheLineStaysInRetentionAndQuotaShare() {
    SiSplit s = AllocationMath.split(bd("10000000"), new Capacity(FORTY, LINE, 3));

    assertThat(s.quotaShare()).isEqualByComparingTo("4000000");
    assertThat(s.retention()).isEqualByComparingTo("6000000");
    assertThat(s.surplus()).isZero();
    assertThat(s.fac()).isZero();
  }

  @Test
  void surplusOnlyTakesTheSumInsuredAboveTheRetention() {
    SiSplit s = AllocationMath.split(bd("60000000"), new Capacity(BigDecimal.ZERO, LINE, 3));

    assertThat(s.retention()).isEqualByComparingTo("25000000");
    assertThat(s.surplus()).isEqualByComparingTo("35000000");
    assertThat(s.fac()).isZero();
    assertThat(new Capacity(BigDecimal.ZERO, LINE, 3).total()).isEqualByComparingTo("100000000");
  }

  @Test
  void quotaShareLimitSendsTheExcessToFacultative() {
    SiSplit s =
        AllocationMath.split(
            bd("40000000"), new Capacity(new BigDecimal("0.5"), bd("30000000"), 0));

    assertThat(s.quotaShare()).isEqualByComparingTo("15000000");
    assertThat(s.retention()).isEqualByComparingTo("15000000");
    assertThat(s.fac()).isEqualByComparingTo("10000000");
  }

  @Test
  void withoutTreatiesEverythingIsRetained() {
    SiSplit s = AllocationMath.split(bd("999"), Capacity.NONE);

    assertThat(s.retention()).isEqualByComparingTo("999");
    assertThat(s.quotaShare().add(s.surplus()).add(s.fac())).isZero();
    assertThat(Capacity.NONE.total()).isNull();
    assertThat(Capacity.NONE.inCurrency(bd("57.85"))).isSameAs(Capacity.NONE);
  }

  @Test
  void capacityIsConvertedToThePolicyCurrency() {
    Capacity usd = new Capacity(FORTY, bd("5785000"), 3).inCurrency(bd("57.85"));

    assertThat(usd.line()).isEqualByComparingTo("100000");
    assertThat(new Capacity(FORTY, LINE, 3).inCurrency(BigDecimal.ONE).line())
        .isEqualByComparingTo(LINE);
  }

  @Test
  void premiumFollowsTheSumInsuredAndProrationKeepsTotals() {
    assertThat(AllocationMath.proportion(bd("120000"), bd("10000000"), bd("120000000")))
        .isEqualByComparingTo("10000.00");
    assertThat(AllocationMath.proportion(bd("100"), bd("1"), BigDecimal.ZERO)).isZero();

    List<BigDecimal> parts =
        AllocationMath.prorate(bd("100.00"), List.of(bd("1"), bd("1"), bd("1")));
    assertThat(parts).containsExactly(bd("33.33"), bd("33.33"), bd("33.34"));
    assertThat(AllocationMath.prorate(bd("10.00"), List.of(BigDecimal.ZERO, BigDecimal.ZERO)))
        .containsExactly(bd("5.00"), bd("5.00"));
    assertThat(AllocationMath.prorate(bd("10.00"), List.of())).isEmpty();
  }

  @Test
  void endorsementIsCededProRataToTheOriginalShares() {
    List<BigDecimal> shares = List.of(bd("50"), bd("30"), bd("20"));
    assertThat(AllocationMath.byPercent(bd("-1000.01"), shares))
        .containsExactly(bd("-500.00"), bd("-300.00"), bd("-200.01"));
    assertThat(AllocationMath.byPercent(bd("1000"), List.of(bd("60"), bd("30"))))
        .containsExactly(bd("600.00"), bd("300.00"));
  }

  @Test
  void excessOfLossRecoversAbovePriorityUpToLimit() {
    assertThat(AllocationMath.excessOfLoss(bd("400000"), bd("500000"), bd("2000000"))).isZero();
    assertThat(AllocationMath.excessOfLoss(bd("900000"), bd("500000"), bd("2000000")))
        .isEqualByComparingTo("400000");
    assertThat(AllocationMath.excessOfLoss(bd("9000000"), bd("500000"), bd("2000000")))
        .isEqualByComparingTo("2000000");
  }

  @Test
  void statementBalanceIsPlacedOnTheSmallerSide() {
    SoaFigures dueTo =
        new SoaFigures(
            bd("1000.00"),
            bd("300.00"),
            bd("5.00"),
            bd("100.00"),
            bd("10.00"),
            bd("200.00"),
            bd("50.00"),
            bd("4.00"),
            bd("30.00"),
            bd("20.00"));
    assertThat(dueTo.income()).isEqualByComparingTo("1084.00");
    assertThat(dueTo.outgo()).isEqualByComparingTo("635.00");
    assertThat(dueTo.balance()).isEqualByComparingTo("449.00");
    assertThat(dueTo.adjustments()).isEqualByComparingTo("161.00");

    SoaLayout layout = SoaLayout.of(dueTo, "Philippine Peso");
    assertThat(layout.balanceOnIncome()).isFalse();
    assertThat(layout.balanceLabel()).isEqualTo("Balance due to reinsurer");
    assertThat(layout.total()).isEqualByComparingTo("1084.00");
    assertThat(layout.lines()).hasSize(10);
    assertThat(layout.amountInWords())
        .isEqualTo("Philippine Peso Four Hundred Forty-Nine and 00/100 only");

    SoaFigures dueFrom =
        new SoaFigures(
            bd("100.00"),
            bd("30.00"),
            BigDecimal.ZERO,
            bd("500.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    SoaLayout from = SoaLayout.of(dueFrom, "Philippine Peso");
    assertThat(from.balanceOnIncome()).isTrue();
    assertThat(from.balance()).isEqualByComparingTo("430.00");
    assertThat(from.total()).isEqualByComparingTo("530.00");
  }

  @Test
  void quartersAndTheirNeighbours() {
    SoaPeriod q1 = new SoaPeriod(2026, 1, LocalDate.of(2026, 4, 15));
    assertThat(q1.from()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(q1.to()).isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(q1.previous().quarter()).isEqualTo(4);
    assertThat(q1.previous().year()).isEqualTo(2025);
    assertThat(new SoaPeriod(2026, 3, null).previous().quarter()).isEqualTo(2);
    assertThat(q1.yearBefore().year()).isEqualTo(2025);
    assertThatThrownBy(() -> new SoaPeriod(2026, 5, null))
        .isInstanceOf(BusinessRuleException.class);
  }
}
