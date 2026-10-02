package com.iortatechnxt.brokerverse.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Dates and numbers as people read them in documents and server texts. */
class DisplayFormatTest {

  @Test
  void writesDatesAsDayMonthNameYear() {
    assertThat(DisplayFormat.date(LocalDate.of(2026, 10, 17))).isEqualTo("17-Oct-2026");
    assertThat(DisplayFormat.date(null)).isEmpty();
    assertThat(DisplayFormat.period(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1)))
        .isEqualTo("01-Nov-2026 to 01-Nov-2027");
    assertThat(DisplayFormat.period(LocalDate.of(2026, 11, 1), null)).isEqualTo("from 01-Nov-2026");
    assertThat(DisplayFormat.period(null, LocalDate.of(2027, 11, 1))).isEqualTo("to 01-Nov-2027");
    assertThat(DisplayFormat.period(null, null)).isEmpty();
  }

  @Test
  void writesTimesInPhilippineTime() {
    assertThat(DisplayFormat.dateTime(Instant.parse("2026-09-27T17:05:00Z")))
        .isEqualTo("28-Sep-2026 01:05");
    assertThat(DisplayFormat.dateTime(null)).isEmpty();
  }

  @Test
  void writesAmountsWithSeparatorsAndTwoDecimals() {
    assertThat(DisplayFormat.amount(new BigDecimal("80000000"))).isEqualTo("80,000,000.00");
    assertThat(DisplayFormat.amount(new BigDecimal("14315.875"))).isEqualTo("14,315.88");
    assertThat(DisplayFormat.amount(null)).isEmpty();
  }

  @Test
  void writesRatesWithTwoToFourDecimals() {
    assertThat(DisplayFormat.rate(new BigDecimal("0.42500000"))).isEqualTo("0.425");
    assertThat(DisplayFormat.rate(new BigDecimal("2"))).isEqualTo("2.00");
    assertThat(DisplayFormat.rate(new BigDecimal("100"))).isEqualTo("100.00");
    assertThat(DisplayFormat.rate(new BigDecimal("1.30000000"))).isEqualTo("1.30");
    assertThat(DisplayFormat.rate(new BigDecimal("0.123456"))).isEqualTo("0.1235");
    // The same cases as formatRate on the screens (frontend utils/format.test.ts).
    assertThat(DisplayFormat.rate(new BigDecimal("1.2"))).isEqualTo("1.20");
    assertThat(DisplayFormat.rate(new BigDecimal("0.12345"))).isEqualTo("0.1235");
    assertThat(DisplayFormat.rate(new BigDecimal("-0.5"))).isEqualTo("-0.50");
    assertThat(DisplayFormat.rate(BigDecimal.ZERO)).isEqualTo("0.00");
    assertThat(DisplayFormat.rate(null)).isEmpty();
    assertThat(DisplayFormat.rate(null)).isEmpty();
  }

  @Test
  void writesPercentWithTwoDecimalsAndTheSign() {
    assertThat(DisplayFormat.percent(new BigDecimal("0.0000"))).isEqualTo("0.00%");
    assertThat(DisplayFormat.percent(new BigDecimal("12.3456"))).isEqualTo("12.35%");
    assertThat(DisplayFormat.percent(new BigDecimal("0.5"))).isEqualTo("0.50%");
    assertThat(DisplayFormat.percent(new BigDecimal("100"))).isEqualTo("100.00%");
    assertThat(DisplayFormat.percent(null)).isEmpty();
  }

  @Test
  void writesAnyValue() {
    assertThat(DisplayFormat.value(LocalDate.of(2026, 1, 5))).isEqualTo("05-Jan-2026");
    assertThat(DisplayFormat.value(new BigDecimal("1500.00"))).isEqualTo("1,500.00");
    assertThat(DisplayFormat.value(new BigDecimal("0.25000000"))).isEqualTo("0.25");
    assertThat(DisplayFormat.value("text")).isEqualTo("text");
    assertThat(DisplayFormat.value(null)).isEmpty();
  }
}
