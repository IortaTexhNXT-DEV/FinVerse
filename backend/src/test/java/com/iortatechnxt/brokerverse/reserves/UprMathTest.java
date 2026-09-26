package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.reserves.domain.EarningUnits;
import com.iortatechnxt.brokerverse.reserves.service.UprMath;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** UPR earning units on the three bases (1/365, 1/24, 1/8). */
class UprMathTest {

  private static EarningUnits units(UprBasis basis, String from, String to, String asOf) {
    return UprMath.units(basis, LocalDate.parse(from), LocalDate.parse(to), LocalDate.parse(asOf));
  }

  @ParameterizedTest(name = "1/365 {0}..{1} at {2}: {3} total, {4} earned")
  @CsvSource({
    // annual policy, end of March: 31 + 28 + 31 = 90 days earned
    "2026-01-01, 2026-12-31, 2026-03-31, 365, 90",
    // leap year: 366 days, 31 + 29 days earned at 29 February
    "2028-01-01, 2028-12-31, 2028-02-29, 366, 60",
    // valuation before the cover starts: nothing earned
    "2026-10-01, 2027-09-30, 2026-09-30, 365, 0",
    // expired cover: fully earned
    "2025-01-01, 2025-12-31, 2026-03-31, 365, 365",
    // one-day cover (from = to) is earned on that day
    "2026-03-31, 2026-03-31, 2026-03-31, 1, 1"
  })
  void dailyProRata(String from, String to, String asOf, int total, int earned) {
    EarningUnits u = units(UprBasis.DAYS_365, from, to, asOf);
    assertThat(u.total()).isEqualTo(total);
    assertThat(u.earned()).isEqualTo(earned);
    assertThat(u.unearned()).isEqualTo(total - earned);
  }

  @Test
  void midPeriodEndorsementEarnsFromItsEffectiveDate() {
    // additional premium effective 1 July on an annual policy: 184 days of cover, half earned
    EarningUnits u = units(UprBasis.DAYS_365, "2026-07-01", "2026-12-31", "2026-09-30");
    assertThat(u.total()).isEqualTo(184);
    assertThat(u.earned()).isEqualTo(92);
    assertThat(u.unearnedFraction()).isEqualByComparingTo("0.5");
  }

  @Test
  void zeroDayCoverHasNoUnits() {
    // cancellation effective after expiry: no day of cover, no UPR
    EarningUnits u = units(UprBasis.DAYS_365, "2027-01-01", "2026-12-31", "2026-06-30");
    assertThat(u.total()).isZero();
    assertThat(u.unearnedFraction()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @ParameterizedTest(name = "1/24 at {0}: {1} earned of 24")
  @CsvSource({
    "2026-01-31, 1",
    "2026-06-30, 11",
    "2026-12-31, 23",
    "2027-01-31, 24",
    "2025-12-31, 0"
  })
  void twentyFourthsOfAnnualPolicy(String asOf, int earned) {
    EarningUnits u = units(UprBasis.TWENTY_FOURTHS, "2026-01-15", "2027-01-14", asOf);
    assertThat(u.total()).isEqualTo(24);
    assertThat(u.earned()).isEqualTo(earned);
  }

  @Test
  void twentyFourthsOfShortTermPolicy() {
    // six months of cover = 12 twenty-fourths; end of the third month: 2 x 3 - 1 = 5 earned
    EarningUnits u = units(UprBasis.TWENTY_FOURTHS, "2026-01-01", "2026-06-30", "2026-03-31");
    assertThat(u.total()).isEqualTo(12);
    assertThat(u.earned()).isEqualTo(5);
    assertThat(u.unearned()).isEqualTo(7);
  }

  @ParameterizedTest(name = "1/8 at {0}: {1} earned of 8")
  @CsvSource({"2026-03-31, 1", "2026-06-30, 3", "2026-12-31, 7", "2027-03-31, 8"})
  void eighthsOfAnnualPolicy(String asOf, int earned) {
    EarningUnits u = units(UprBasis.EIGHTHS, "2026-02-10", "2027-02-09", asOf);
    assertThat(u.total()).isEqualTo(8);
    assertThat(u.earned()).isEqualTo(earned);
  }

  @Test
  void monthlyBasesEarnNothingBeforeTheStart() {
    assertThat(units(UprBasis.EIGHTHS, "2026-03-20", "2027-03-19", "2026-03-10").earned()).isZero();
    assertThat(units(UprBasis.TWENTY_FOURTHS, "2026-03-20", "2027-03-19", "2026-03-10").earned())
        .isZero();
  }

  @Test
  void partialMonthCountsAsAWholeMonth() {
    // 45 days = 1 whole month + 14 days: two months, four twenty-fourths
    assertThat(units(UprBasis.TWENTY_FOURTHS, "2026-01-01", "2026-02-14", "2026-01-31").total())
        .isEqualTo(4);
  }
}
