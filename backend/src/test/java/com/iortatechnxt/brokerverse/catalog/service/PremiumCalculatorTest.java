package com.iortatechnxt.brokerverse.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.Period;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatedItem;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Appendix A worked examples, including every DST rounding edge. */
class PremiumCalculatorTest {

  private static final PremiumCalculator CALC = new PremiumCalculator();

  /** Fire as Appendix A reads: DST 12.5, PTX 12, FST 2, LGT 0.75 (branch), commission 20. */
  private static final RatingRates FIRE =
      new RatingRates(
          bd("12.5"), bd("12"), bd("0"), bd("2"), bd("0.75"), bd("20"), bd("12"), null, null);

  /** Motor: DST 12.5, VAT 12, LGT 0.75, commission 15, OD factors 90 / 81. */
  private static final RatingRates MOTOR =
      new RatingRates(
          bd("12.5"), null, bd("12"), null, bd("0.75"), bd("15"), bd("12"), bd("90"), bd("81"));

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static PremiumRequest fire(String tsi, String rate, Period period, String minimum) {
    return new PremiumRequest(
        RatingMethod.PROPERTY,
        List.of(RatedItem.of("Building", bd(tsi), bd(rate))),
        FIRE,
        false,
        period,
        minimum == null ? null : bd(minimum),
        false);
  }

  @Nested
  class Fire {

    @Test
    void netChargesGrossAndCommissionAsInAppendixA() {
      PremiumBreakdown b = CALC.calculate(fire("1000000", "0.25", Period.ANNUAL, "1000"));
      assertThat(b.method()).isEqualTo(RatingMethod.PROPERTY);
      assertThat(b.sumInsured()).isEqualByComparingTo("1000000.00");
      assertThat(b.annualPremium()).isEqualByComparingTo("2500.00");
      assertThat(b.netPremium()).isEqualByComparingTo("2500.00");
      assertThat(b.dst()).isEqualByComparingTo("312.50");
      assertThat(b.premiumTax()).isEqualByComparingTo("300.00");
      assertThat(b.fst()).isEqualByComparingTo("50.00");
      assertThat(b.lgt()).isEqualByComparingTo("18.75");
      assertThat(b.vat()).isEqualByComparingTo("0.00");
      assertThat(b.totalCharges()).isEqualByComparingTo("681.25");
      assertThat(b.grossPremium()).isEqualByComparingTo("3181.25");
      assertThat(b.commission()).isEqualByComparingTo("500.00");
      assertThat(b.vatOnCommission()).isEqualByComparingTo("60.00");
      assertThat(b.minimumApplied()).isFalse();
      assertThat(b.periodFactor()).isEqualByComparingTo("1");
      assertThat(b.odTheftCoverage()).isEqualByComparingTo("0");
      assertThat(b.items())
          .singleElement()
          .satisfies(i -> assertThat(i.premium()).isEqualByComparingTo("2500.00"));
    }

    @Test
    void dstOfAnOddNetPremiumRoundsUpToTheHalfPeso() {
      // net 1,234,567 x 0.25 % = 3,086.42 (3,086.4175); DST 385.8025 -> 385.80 -> 386.00
      PremiumBreakdown b = CALC.calculate(fire("1234567", "0.25", Period.ANNUAL, null));
      assertThat(b.netPremium()).isEqualByComparingTo("3086.42");
      assertThat(b.dst()).isEqualByComparingTo("386.00");
      assertThat(b.premiumTax()).isEqualByComparingTo("370.37");
      assertThat(b.fst()).isEqualByComparingTo("61.73");
      assertThat(b.lgt()).isEqualByComparingTo("23.15");
      assertThat(b.totalCharges()).isEqualByComparingTo("841.25");
      assertThat(b.grossPremium()).isEqualByComparingTo("3927.67");
    }

    @Test
    void severalLocationsAddUp() {
      PremiumRequest request =
          new PremiumRequest(
              RatingMethod.PROPERTY,
              List.of(
                  RatedItem.of("Building", bd("2000000"), bd("0.25")),
                  RatedItem.of("Contents", bd("500000"), bd("0.30"))),
              FIRE,
              false,
              Period.ANNUAL,
              BigDecimal.ZERO,
              false);
      PremiumBreakdown b = CALC.calculate(request);
      assertThat(b.sumInsured()).isEqualByComparingTo("2500000.00");
      assertThat(b.netPremium()).isEqualByComparingTo("6500.00");
      assertThat(b.items())
          .extracting(PremiumBreakdown.ItemPremium::premium)
          .usingElementComparator(BigDecimal::compareTo)
          .containsExactly(bd("5000.00"), bd("1500.00"));
    }
  }

  @Nested
  class Motor {

    private PremiumRequest motor(boolean multiYear, String bi, String pd) {
      return new PremiumRequest(
          RatingMethod.MOTOR,
          List.of(new RatedItem("ABC1234", bd("1000000"), bd("1.3"), bd(bi), bd(pd))),
          MOTOR,
          multiYear,
          Period.ANNUAL,
          bd("5000"),
          false);
    }

    @Test
    void annualCoverUsesNinetyPercentOfTsi() {
      PremiumBreakdown b = CALC.calculate(motor(false, "285", "440"));
      assertThat(b.odTheftCoverage()).isEqualByComparingTo("900000.00");
      assertThat(b.odTheftPremium()).isEqualByComparingTo("11700.00");
      assertThat(b.biPremium()).isEqualByComparingTo("285.00");
      assertThat(b.pdPremium()).isEqualByComparingTo("440.00");
      assertThat(b.netPremium()).isEqualByComparingTo("12425.00");
      // DST 1,553.125 -> 1,553.13 -> 1,553.50
      assertThat(b.dst()).isEqualByComparingTo("1553.50");
      assertThat(b.vat()).isEqualByComparingTo("1491.00");
      assertThat(b.lgt()).isEqualByComparingTo("93.19");
      assertThat(b.premiumTax()).isEqualByComparingTo("0.00");
      assertThat(b.fst()).isEqualByComparingTo("0.00");
      assertThat(b.totalCharges()).isEqualByComparingTo("3137.69");
      assertThat(b.grossPremium()).isEqualByComparingTo("15562.69");
      assertThat(b.commission()).isEqualByComparingTo("1863.75");
      assertThat(b.vatOnCommission()).isEqualByComparingTo("223.65");
      assertThat(b.items().get(0).premium()).isEqualByComparingTo("12425.00");
    }

    @Test
    void multiYearCoverUsesEightyOnePercentOfTsi() {
      PremiumBreakdown b = CALC.calculate(motor(true, "285", "440"));
      assertThat(b.odTheftCoverage()).isEqualByComparingTo("810000.00");
      assertThat(b.odTheftPremium()).isEqualByComparingTo("10530.00");
      assertThat(b.netPremium()).isEqualByComparingTo("11255.00");
      // DST 1,406.875 -> 1,406.88 -> 1,407.00
      assertThat(b.dst()).isEqualByComparingTo("1407.00");
    }

    @Test
    void fleetAddsEveryVehicle() {
      PremiumRequest request =
          new PremiumRequest(
              RatingMethod.MOTOR,
              List.of(
                  new RatedItem("V1", bd("800000"), bd("1.5"), null, null),
                  new RatedItem("V2", bd("600000"), bd("1.5"), bd("170"), null)),
              MOTOR,
              false,
              Period.ANNUAL,
              null,
              false);
      PremiumBreakdown b = CALC.calculate(request);
      assertThat(b.odTheftCoverage()).isEqualByComparingTo("1260000.00");
      assertThat(b.odTheftPremium()).isEqualByComparingTo("18900.00");
      assertThat(b.biPremium()).isEqualByComparingTo("170.00");
      assertThat(b.netPremium()).isEqualByComparingTo("19070.00");
    }

    @Test
    void missingOdFactorIsReported() {
      RatingRates noFactor =
          new RatingRates(bd("12.5"), null, bd("12"), null, null, null, null, null, null);
      PremiumRequest request =
          new PremiumRequest(
              RatingMethod.MOTOR,
              List.of(new RatedItem("V1", bd("800000"), bd("1.5"), null, null)),
              noFactor,
              false,
              Period.ANNUAL,
              null,
              false);
      assertThatThrownBy(() -> CALC.calculate(request))
          .extracting("code")
          .isEqualTo("RATING_OD_FACTOR_MISSING");
    }
  }

  @Nested
  class OtherLines {

    @Test
    void genericNetIsTheSumOfItemsTimesRate() {
      RatingRates rates =
          new RatingRates(
              bd("12.5"), null, bd("12"), null, bd("0.5"), bd("15"), bd("12"), null, null);
      PremiumRequest request =
          new PremiumRequest(
              RatingMethod.GENERIC,
              List.of(
                  RatedItem.of("Premises", bd("5000000"), bd("0.25")),
                  RatedItem.of("Operations", bd("2000000"), bd("0.1"))),
              rates,
              false,
              null,
              bd("1500"),
              false);
      PremiumBreakdown b = CALC.calculate(request);
      assertThat(b.netPremium()).isEqualByComparingTo("14500.00");
      assertThat(b.dst()).isEqualByComparingTo("1812.50");
      assertThat(b.vat()).isEqualByComparingTo("1740.00");
      assertThat(b.lgt()).isEqualByComparingTo("72.50");
      assertThat(b.grossPremium()).isEqualByComparingTo("18125.00");
      assertThat(b.commission()).isEqualByComparingTo("2175.00");
      assertThat(b.vatOnCommission()).isEqualByComparingTo("261.00");
    }

    @Test
    void missingRatesCountAsZero() {
      RatingRates none = new RatingRates(null, null, null, null, null, null, null, null, null);
      PremiumRequest request =
          new PremiumRequest(
              RatingMethod.GENERIC,
              List.of(RatedItem.of("Risk", bd("100000"), bd("1"))),
              none,
              false,
              Period.ANNUAL,
              null,
              false);
      PremiumBreakdown b = CALC.calculate(request);
      assertThat(b.netPremium()).isEqualByComparingTo("1000.00");
      assertThat(b.totalCharges()).isEqualByComparingTo("0.00");
      assertThat(b.grossPremium()).isEqualByComparingTo("1000.00");
      assertThat(b.commission()).isEqualByComparingTo("0.00");
    }

    @Test
    void itemsAreRequiredAndSumsInsuredCannotBeNegative() {
      PremiumRequest empty =
          new PremiumRequest(RatingMethod.GENERIC, null, FIRE, false, null, null, false);
      assertThatThrownBy(() -> CALC.calculate(empty))
          .extracting("code")
          .isEqualTo("RATING_NO_ITEMS");
      PremiumRequest negative =
          new PremiumRequest(
              RatingMethod.GENERIC,
              List.of(RatedItem.of("Risk", bd("-1"), bd("1"))),
              FIRE,
              false,
              null,
              null,
              false);
      assertThatThrownBy(() -> CALC.calculate(negative))
          .extracting("code")
          .isEqualTo("RATING_SUM_INSURED_INVALID");
    }
  }

  @Nested
  class PeriodsAndMinimum {

    @Test
    void proRataUsesDaysOverThreeSixtyFive() {
      Period period =
          new Period(
              PeriodBasis.PRO_RATA, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 2), null);
      PremiumBreakdown b = CALC.calculate(fire("1000000", "0.25", period, "5000"));
      // 182 / 365 = 0.4986301370; 2,500 x factor = 1,246.58; minimum never applies to pro-rata
      assertThat(b.periodFactor()).isEqualByComparingTo("0.4986301370");
      assertThat(b.netPremium()).isEqualByComparingTo("1246.58");
      assertThat(b.minimumApplied()).isFalse();
      assertThat(b.dst()).isEqualByComparingTo("156.00");
    }

    @Test
    void proRataUsesThreeSixtySixWhenThePolicyYearHasTheTwentyNinthOfFebruary() {
      assertThat(
              PremiumCalculator.proRataFactor(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 7, 1)))
          .isEqualByComparingTo("0.4972677596");
      assertThat(
              PremiumCalculator.proRataFactor(LocalDate.of(2027, 3, 1), LocalDate.of(2028, 3, 1)))
          .isEqualByComparingTo("1");
      assertThat(
              PremiumCalculator.proRataFactor(LocalDate.of(2026, 3, 1), LocalDate.of(2027, 3, 1)))
          .isEqualByComparingTo("1");
    }

    @Test
    void proRataNeedsAValidPeriod() {
      LocalDate day = LocalDate.of(2026, 1, 1);
      assertThatThrownBy(() -> PremiumCalculator.proRataFactor(day, day))
          .extracting("code")
          .isEqualTo("RATING_PERIOD_INVALID");
      assertThatThrownBy(() -> PremiumCalculator.proRataFactor(null, day))
          .extracting("code")
          .isEqualTo("RATING_PERIOD_INVALID");
    }

    @Test
    void shortPeriodAppliesTheTablePercentage() {
      Period period = new Period(PeriodBasis.SHORT_PERIOD, null, null, bd("40"));
      PremiumBreakdown b = CALC.calculate(fire("1000000", "0.25", period, null));
      assertThat(b.periodFactor()).isEqualByComparingTo("0.4");
      assertThat(b.netPremium()).isEqualByComparingTo("1000.00");
      assertThat(b.annualPremium()).isEqualByComparingTo("2500.00");
    }

    @Test
    void shortPeriodNeedsAPercentage() {
      Period missing = new Period(PeriodBasis.SHORT_PERIOD, null, null, null);
      assertThatThrownBy(() -> CALC.calculate(fire("1000", "1", missing, null)))
          .extracting("code")
          .isEqualTo("RATING_SHORT_PERIOD_INVALID");
      Period tooHigh = new Period(PeriodBasis.SHORT_PERIOD, null, null, bd("101"));
      assertThatThrownBy(() -> CALC.calculate(fire("1000", "1", tooHigh, null)))
          .extracting("code")
          .isEqualTo("RATING_SHORT_PERIOD_INVALID");
    }

    @Test
    void minimumPremiumReplacesALowerNetPremium() {
      PremiumBreakdown b = CALC.calculate(fire("100000", "0.25", Period.ANNUAL, "1000"));
      assertThat(b.annualPremium()).isEqualByComparingTo("250.00");
      assertThat(b.minimumApplied()).isTrue();
      assertThat(b.netPremium()).isEqualByComparingTo("1000.00");
      assertThat(b.dst()).isEqualByComparingTo("125.00");
      assertThat(b.commission()).isEqualByComparingTo("200.00");
    }

    @Test
    void minimumPremiumAppliesToShortPeriodButNotToEndorsements() {
      Period shortPeriod = new Period(PeriodBasis.SHORT_PERIOD, null, null, bd("20"));
      assertThat(CALC.calculate(fire("1000000", "0.25", shortPeriod, "1000")).netPremium())
          .isEqualByComparingTo("1000.00");
      PremiumRequest endorsement =
          new PremiumRequest(
              RatingMethod.PROPERTY,
              List.of(RatedItem.of("Building", bd("100000"), bd("0.25"))),
              FIRE,
              false,
              Period.ANNUAL,
              bd("1000"),
              true);
      PremiumBreakdown b = CALC.calculate(endorsement);
      assertThat(b.minimumApplied()).isFalse();
      assertThat(b.netPremium()).isEqualByComparingTo("250.00");
    }

    @Test
    void endorsementsAreRatedForTheRemainingTermAndMayReturnPremium() {
      Period remaining =
          Period.remainingTerm(
              PeriodBasis.PRO_RATA, LocalDate.of(2026, 7, 1), LocalDate.of(2027, 1, 1), null);
      PremiumRequest increase =
          new PremiumRequest(
              RatingMethod.PROPERTY,
              List.of(RatedItem.of("Additional contents", bd("200000"), bd("0.25"))),
              FIRE,
              false,
              remaining,
              bd("1000"),
              true);
      // 500 x 184/365 = 252.05; no minimum premium on an endorsement
      PremiumBreakdown up = CALC.calculate(increase);
      assertThat(up.netPremium()).isEqualByComparingTo("252.05");
      assertThat(up.minimumApplied()).isFalse();
      assertThat(up.dst()).isEqualByComparingTo("32.00");

      PremiumRequest reduction =
          new PremiumRequest(
              RatingMethod.PROPERTY,
              List.of(RatedItem.of("Contents removed", bd("-200000"), bd("0.25"))),
              FIRE,
              false,
              remaining,
              bd("1000"),
              true);
      PremiumBreakdown down = CALC.calculate(reduction);
      assertThat(down.netPremium()).isEqualByComparingTo("-252.05");
      assertThat(down.dst()).isEqualByComparingTo("-32.00");
      assertThat(down.grossPremium()).isNegative();
      assertThat(down.commission()).isEqualByComparingTo("-50.41");

      Period shortRemaining =
          Period.remainingTerm(
              PeriodBasis.SHORT_PERIOD,
              LocalDate.of(2026, 10, 1),
              LocalDate.of(2027, 1, 1),
              bd("40"));
      PremiumRequest shortTerm =
          new PremiumRequest(
              RatingMethod.PROPERTY,
              List.of(RatedItem.of("Building increase", bd("1000000"), bd("0.25"))),
              FIRE,
              false,
              shortRemaining,
              null,
              true);
      assertThat(CALC.calculate(shortTerm).netPremium()).isEqualByComparingTo("1000.00");
    }

    @Test
    void anEqualNetPremiumIsNotRaised() {
      PremiumBreakdown b = CALC.calculate(fire("400000", "0.25", Period.ANNUAL, "1000"));
      assertThat(b.minimumApplied()).isFalse();
      assertThat(b.netPremium()).isEqualByComparingTo("1000.00");
    }
  }

  @Nested
  class DstRounding {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "100.00, 100.00",
      "100.01, 100.50",
      "100.25, 100.50",
      "100.49, 100.50",
      "100.50, 100.50",
      "100.51, 101.00",
      "100.75, 101.00",
      "100.99, 101.00",
      "0.00, 0.00",
      "0.01, 0.50",
      "0.004, 0.00",
      "0.005, 0.50",
      "100.494, 100.50",
      "100.495, 100.50",
      "100.504, 100.50",
      "100.505, 101.00",
      "1553.125, 1553.50",
      "1406.875, 1407.00",
      "-100.01, -100.50",
      "-100.51, -101.00",
      "-100.50, -100.50",
    })
    void halfPesoRule(String raw, String expected) {
      assertThat(PremiumCalculator.roundDst(bd(raw))).isEqualTo(bd(expected));
    }

    @Test
    void nullIsZero() {
      assertThat(PremiumCalculator.roundDst(null)).isEqualTo(bd("0.00"));
    }
  }

  @Nested
  class MonthsCovered {

    @ParameterizedTest(name = "{0} to {1} = {2} months")
    @CsvSource({
      "2026-01-01, 2026-01-15, 1",
      "2026-01-01, 2026-02-01, 1",
      "2026-01-01, 2026-02-02, 2",
      "2026-01-31, 2026-04-30, 3",
      "2026-01-01, 2026-07-01, 6",
      "2026-01-01, 2027-01-01, 12",
      "2026-01-01, 2027-06-01, 12",
    })
    void startedMonthsCount(String from, String to, int months) {
      assertThat(RatingService.monthsCovered(LocalDate.parse(from), LocalDate.parse(to)))
          .isEqualTo(months);
    }

    @Test
    void periodMustEndAfterItStarts() {
      LocalDate day = LocalDate.of(2026, 5, 1);
      assertThatThrownBy(() -> RatingService.monthsCovered(day, day.minusDays(1)))
          .extracting("code")
          .isEqualTo("RATING_PERIOD_INVALID");
    }
  }
}
