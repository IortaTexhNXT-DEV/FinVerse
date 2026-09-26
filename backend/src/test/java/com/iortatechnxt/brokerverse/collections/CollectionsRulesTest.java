package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.collections.common.domain.AgingBrackets;
import com.iortatechnxt.brokerverse.collections.files.service.FilePeriods;
import com.iortatechnxt.brokerverse.collections.files.service.FilePeriods.Period;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Pure rules of the Collections core: aging brackets (OQ43) and file periods (BRCLXN.024-029). */
class CollectionsRulesTest {

  @Test
  void agingBracketsPlaceEveryAge() {
    AgingBrackets brackets =
        AgingBrackets.parse(List.of("0-30", "31-45", "46-60", "61-90", "91-120", "121+"));
    assertThat(brackets.labelOf(-3)).isEqualTo("0-30");
    assertThat(brackets.labelOf(0)).isEqualTo("0-30");
    assertThat(brackets.labelOf(31)).isEqualTo("31-45");
    assertThat(brackets.labelOf(120)).isEqualTo("91-120");
    assertThat(brackets.labelOf(500)).isEqualTo("121+");
    assertThat(brackets.all()).hasSize(6);
    assertThat(AgingBrackets.parse(List.of("0-60", "61-")).labelOf(90)).isEqualTo("61+");
    assertThat(AgingBrackets.parse(List.of("0-30")).labelOf(45)).isEqualTo("0-30");
    assertThat(AgingBrackets.parse(List.of()).labelOf(10)).isEqualTo("0+");
  }

  @Test
  void invalidAgingBracketsAreRefused() {
    assertThatThrownBy(() -> AgingBrackets.parse(List.of("0-30", "20-40")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ascending");
    assertThatThrownBy(() -> AgingBrackets.parse(List.of("30-10")))
        .hasMessageContaining("ends before");
    assertThatThrownBy(() -> AgingBrackets.parse(List.of("abc"))).hasMessageContaining("from-to");
  }

  @Test
  void theWeekRunsSaturdayToFridayAndIsAvailableMonday() {
    Period week = FilePeriods.weekEnding(LocalDate.of(2026, 9, 29));
    assertThat(week.to()).isEqualTo(LocalDate.of(2026, 9, 25));
    assertThat(week.from()).isEqualTo(LocalDate.of(2026, 9, 19));
    assertThat(week.from().getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
    assertThat(week.key()).isEqualTo("2026-W39");
    assertThat(FilePeriods.mondayAfter(week)).isEqualTo(Instant.parse("2026-09-28T00:00:00Z"));
    assertThat(FilePeriods.weekEnding(LocalDate.of(2026, 9, 25)).to())
        .isEqualTo(LocalDate.of(2026, 9, 25));
  }

  @Test
  void theMonthlyFileIsForThePreviousMonthOnTheFirstWorkingDay() {
    Period month = FilePeriods.previousMonth(LocalDate.of(2026, 3, 2));
    assertThat(month.key()).isEqualTo("2026-02");
    assertThat(month.to()).isEqualTo(LocalDate.of(2026, 2, 28));
    Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 11, 2));
    Predicate<LocalDate> working =
        d ->
            d.getDayOfWeek() != DayOfWeek.SATURDAY
                && d.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(d);
    assertThat(FilePeriods.isFirstWorkingDay(LocalDate.of(2026, 11, 3), working)).isTrue();
    assertThat(FilePeriods.isFirstWorkingDay(LocalDate.of(2026, 11, 2), working)).isFalse();
    assertThat(FilePeriods.isFirstWorkingDay(LocalDate.of(2026, 11, 4), working)).isFalse();
    assertThat(FilePeriods.isFirstWorkingDay(LocalDate.of(2026, 10, 1), working)).isTrue();
    assertThat(FilePeriods.officeOpens(LocalDate.of(2026, 10, 1)))
        .isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
  }
}
