package com.iortatechnxt.brokerverse.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecurrenceFrequencyTest {

  private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);

  @Test
  void monthlyMonthEndFollowsShortMonths() {
    assertThat(RecurrenceFrequency.MONTHLY.occurrences(31, JAN_1, null, LocalDate.of(2026, 4, 30)))
        .containsExactly(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30));
  }

  @Test
  void occurrencesBeforeStartOrAfterEndAreExcluded() {
    LocalDate start = LocalDate.of(2026, 1, 20);
    assertThat(
            RecurrenceFrequency.MONTHLY.occurrences(
                10, start, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 12, 31)))
        .containsExactly(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10));
  }

  @Test
  void quarterlyAndAnnualSchedulesStepFromTheStartMonth() {
    assertThat(
            RecurrenceFrequency.QUARTERLY.occurrences(15, JAN_1, null, LocalDate.of(2026, 12, 31)))
        .containsExactly(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 4, 15),
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2026, 10, 15));
    assertThat(
            RecurrenceFrequency.ANNUALLY.occurrences(
                29, LocalDate.of(2024, 2, 1), null, LocalDate.of(2026, 12, 31)))
        .containsExactly(
            LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28));
    assertThat(RecurrenceFrequency.QUARTERLY.months()).isEqualTo(3);
  }

  @Test
  void nextOccurrence() {
    assertThat(RecurrenceFrequency.MONTHLY.nextAfter(5, JAN_1, null, LocalDate.of(2026, 3, 5)))
        .isEqualTo(LocalDate.of(2026, 4, 5));
    assertThat(
            RecurrenceFrequency.MONTHLY.nextAfter(
                5, JAN_1, LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 5)))
        .isNull();
    assertThat(
            RecurrenceFrequency.MONTHLY.nextAfter(
                5, LocalDate.of(2026, 6, 20), null, LocalDate.of(2026, 1, 1)))
        .isEqualTo(LocalDate.of(2026, 7, 5));
  }
}
