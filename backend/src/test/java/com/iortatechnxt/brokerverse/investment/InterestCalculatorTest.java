package com.iortatechnxt.brokerverse.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.iortatechnxt.brokerverse.investment.domain.AmortizationMethod;
import com.iortatechnxt.brokerverse.investment.domain.CouponFrequency;
import com.iortatechnxt.brokerverse.investment.domain.DayCountConvention;
import com.iortatechnxt.brokerverse.investment.domain.HoldingTerms;
import com.iortatechnxt.brokerverse.investment.report.MaturityProfileReport;
import com.iortatechnxt.brokerverse.investment.service.InterestCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InterestCalculatorTest {

  private static final LocalDate SETTLE = LocalDate.of(2026, 3, 15);
  private static final LocalDate MATURITY = LocalDate.of(2031, 3, 15);

  private static HoldingTerms bond(
      String price, DayCountConvention dayCount, AmortizationMethod method) {
    return new HoldingTerms(
        new BigDecimal("1000000"),
        new BigDecimal(price),
        BigDecimal.ZERO,
        SETTLE,
        SETTLE,
        MATURITY,
        new BigDecimal("6"),
        CouponFrequency.SEMI_ANNUAL,
        dayCount,
        method);
  }

  @Test
  void thirtyE360CountsThirtyDayMonthsAndIsAdditive() {
    DayCountConvention c = DayCountConvention.THIRTY_360;
    assertThat(c.days(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 31))).isEqualTo(15);
    assertThat(c.days(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30))).isEqualTo(30);
    assertThat(c.days(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28))).isEqualTo(28);
    assertThat(c.days(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 9, 15))).isEqualTo(180);
    assertThat(DayCountConvention.ACT_365.days(SETTLE, LocalDate.of(2026, 9, 15))).isEqualTo(184);
  }

  @Test
  void couponInterestFollowsTheDayCount() {
    HoldingTerms thirty = bond("1000000", DayCountConvention.THIRTY_360, AmortizationMethod.NONE);
    assertThat(InterestCalculator.couponInterest(thirty, SETTLE, LocalDate.of(2026, 3, 31)))
        .isEqualByComparingTo("2500.00");
    assertThat(InterestCalculator.couponInterest(thirty, SETTLE, LocalDate.of(2026, 9, 15)))
        .isEqualByComparingTo("30000.00");
    HoldingTerms act = bond("1000000", DayCountConvention.ACT_365, AmortizationMethod.NONE);
    assertThat(
            InterestCalculator.couponInterest(
                act, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
        .isEqualByComparingTo("4767.12");
    assertThat(InterestCalculator.couponInterest(act, SETTLE, SETTLE)).isZero();
  }

  @Test
  void effectiveRateAmortizesDiscountToFaceValueAtMaturity() {
    HoldingTerms t =
        bond("960000", DayCountConvention.THIRTY_360, AmortizationMethod.EFFECTIVE_INTEREST);
    BigDecimal rate = InterestCalculator.effectiveRate(t, SETTLE, t.purchasePrice());
    assertThat(rate.doubleValue()).isBetween(6.9, 7.2);
    BigDecimal beforeMaturity =
        InterestCalculator.carryingAt(t, rate, t.purchasePrice(), SETTLE, MATURITY.minusDays(1));
    assertThat(beforeMaturity.doubleValue()).isCloseTo(1_000_000, within(50.0));
    BigDecimal atMaturity =
        InterestCalculator.carryingAt(t, rate, t.purchasePrice(), SETTLE, MATURITY);
    assertThat(atMaturity).isEqualByComparingTo("1000000.00");
    BigDecimal firstMonth =
        InterestCalculator.amortization(
            t, rate, t.purchasePrice(), SETTLE, LocalDate.of(2026, 3, 31));
    assertThat(firstMonth.signum()).isPositive();
  }

  @Test
  void premiumIsAmortizedNegativelyAndStraightLineIsPerDay() {
    HoldingTerms premium =
        new HoldingTerms(
            new BigDecimal("1000000"),
            new BigDecimal("1012000"),
            BigDecimal.ZERO,
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2027, 6, 30),
            new BigDecimal("7"),
            CouponFrequency.ANNUAL,
            DayCountConvention.ACT_365,
            AmortizationMethod.STRAIGHT_LINE);
    BigDecimal july =
        InterestCalculator.amortization(
            premium,
            null,
            premium.purchasePrice(),
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 31));
    assertThat(july).isEqualByComparingTo("-1019.18");
    BigDecimal eir =
        InterestCalculator.effectiveRate(
            premium, premium.settlementDate(), premium.purchasePrice());
    assertThat(eir.doubleValue()).isBetween(5.5, 6.0);
  }

  @Test
  void couponDatesCountBackFromMaturity() {
    HoldingTerms t = bond("1000000", DayCountConvention.THIRTY_360, AmortizationMethod.NONE);
    assertThat(InterestCalculator.lastCouponDate(t, LocalDate.of(2026, 1, 1)))
        .isEqualTo(LocalDate.of(2025, 9, 15));
    assertThat(
            InterestCalculator.couponDates(t, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
        .containsExactly(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 9, 15));
    HoldingTerms deposit =
        new HoldingTerms(
            new BigDecimal("1000000"),
            new BigDecimal("1000000"),
            BigDecimal.ZERO,
            SETTLE,
            SETTLE,
            LocalDate.of(2026, 9, 15),
            new BigDecimal("5"),
            CouponFrequency.AT_MATURITY,
            DayCountConvention.ACT_365,
            AmortizationMethod.NONE);
    assertThat(InterestCalculator.lastCouponDate(deposit, LocalDate.of(2026, 6, 1)))
        .isEqualTo(SETTLE);
    assertThat(InterestCalculator.couponDates(deposit, SETTLE, LocalDate.of(2026, 12, 31)))
        .containsExactly(LocalDate.of(2026, 9, 15));
    assertThat(InterestCalculator.amortization(deposit, null, BigDecimal.TEN, SETTLE, MATURITY))
        .isZero();
  }

  @Test
  void maturityBucketsCoverTheWholeRange() {
    LocalDate asOf = LocalDate.of(2026, 9, 30);
    assertThat(MaturityProfileReport.bucket(asOf, null)).isEqualTo(8);
    assertThat(MaturityProfileReport.bucket(asOf, asOf.minusDays(3))).isZero();
    assertThat(MaturityProfileReport.bucket(asOf, asOf.plusDays(10))).isEqualTo(1);
    assertThat(MaturityProfileReport.bucket(asOf, asOf.plusDays(60))).isEqualTo(2);
    assertThat(MaturityProfileReport.bucket(asOf, asOf.plusDays(400))).isEqualTo(5);
    assertThat(MaturityProfileReport.bucket(asOf, asOf.plusYears(10))).isEqualTo(7);
    assertThat(MaturityProfileReport.label(7)).isEqualTo("Over 5 years");
  }
}
