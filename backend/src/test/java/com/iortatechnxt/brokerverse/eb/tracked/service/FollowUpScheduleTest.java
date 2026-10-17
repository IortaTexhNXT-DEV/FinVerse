package com.iortatechnxt.brokerverse.eb.tracked.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.eb.tracked.service.FollowUpSchedule.Step;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** The follow-up schedule of tracked items (FR-EB-057 R1, R2). */
class FollowUpScheduleTest {

  private static final Predicate<LocalDate> WEEKDAYS =
      d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;

  private final FollowUpSchedule schedule = new FollowUpSchedule(5, 3);

  // Friday 2-Oct-2026
  private final LocalDate due = LocalDate.of(2026, 10, 2);

  @Test
  void nothingIsDueBeforeTheIntervalAfterTheDueDate() {
    assertThat(schedule.stepOn(due, 0, false, due, WEEKDAYS)).isEqualTo(Step.NONE);
    assertThat(schedule.stepOn(due, 0, false, LocalDate.of(2026, 10, 8), WEEKDAYS))
        .isEqualTo(Step.NONE);
    assertThat(schedule.nextDate(due, 0, WEEKDAYS)).isEqualTo(LocalDate.of(2026, 10, 9));
  }

  @Test
  void followUpsComeEveryIntervalThenTheEscalation() {
    assertThat(schedule.stepOn(due, 0, false, LocalDate.of(2026, 10, 9), WEEKDAYS))
        .isEqualTo(Step.FOLLOW_UP);
    assertThat(schedule.stepOn(due, 1, false, LocalDate.of(2026, 10, 9), WEEKDAYS))
        .isEqualTo(Step.NONE);
    assertThat(schedule.stepOn(due, 2, false, LocalDate.of(2026, 10, 23), WEEKDAYS))
        .isEqualTo(Step.FOLLOW_UP);
    assertThat(schedule.stepOn(due, 3, false, LocalDate.of(2026, 10, 30), WEEKDAYS))
        .isEqualTo(Step.ESCALATE);
    assertThat(schedule.stepOn(due, 3, true, LocalDate.of(2026, 11, 30), WEEKDAYS))
        .isEqualTo(Step.NONE);
  }

  @Test
  void aZeroIntervalCountsAsOneWorkingDayAndHolidaysAreSkipped() {
    FollowUpSchedule daily = new FollowUpSchedule(0, 1);
    Predicate<LocalDate> withHoliday =
        d -> WEEKDAYS.test(d) && !d.equals(LocalDate.of(2026, 10, 5));
    assertThat(daily.nextDate(due, 0, withHoliday)).isEqualTo(LocalDate.of(2026, 10, 6));
    assertThat(EbWorkingDays.plus(due, 0, WEEKDAYS)).isEqualTo(due);
  }
}
