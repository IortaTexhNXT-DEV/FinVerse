package com.iortatechnxt.brokerverse.nbadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** Working hours of the out-of-hours risk flag (UAM-NFR-40, UQ07). */
class WorkingHoursTest {

  private static ZonedDateTime manila(int day, int hour, int minute) {
    // September 2026: the 21st is a Monday.
    return LocalDateTime.of(2026, 9, day, hour, minute).atZone(WorkingHours.ZONE);
  }

  @Test
  void deliveredValueCoversWeekdaysFromEightToSix() {
    WorkingHours hours = WorkingHours.parse("08:00-18:00,MON-FRI");
    assertThat(hours.days()).hasSize(5).doesNotContain(DayOfWeek.SATURDAY);
    assertThat(hours.contains(manila(21, 17, 59))).isTrue();
    assertThat(hours.contains(manila(21, 18, 0))).isFalse();
    assertThat(hours.contains(manila(22, 21, 0))).isFalse();
    assertThat(hours.contains(manila(21, 7, 59))).isFalse();
    assertThat(hours.contains(manila(26, 10, 0))).isFalse();
    assertThat(hours.contains(manila(21, 9, 0).withZoneSameInstant(java.time.ZoneOffset.UTC)))
        .isTrue();
  }

  @Test
  void listsWrapsAndInvalidValuesAreRead() {
    assertThat(WorkingHours.parse("00:00-24:00,MON-SUN").contains(manila(27, 23, 59))).isTrue();
    assertThat(WorkingHours.parse("09:00-17:00,SAT,SUN").days())
        .containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    assertThat(WorkingHours.parse("09:00-17:00,FRI-MON").days())
        .containsExactlyInAnyOrder(
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY);
    assertThat(WorkingHours.parse("09:00-17:00").days()).hasSize(7);
    assertThat(WorkingHours.parse("")).isEqualTo(WorkingHours.ALWAYS);
    assertThat(WorkingHours.parse("nine to five")).isEqualTo(WorkingHours.ALWAYS);
    assertThat(WorkingHours.parse("09:00-17:00,XYZ")).isEqualTo(WorkingHours.ALWAYS);
    assertThat(WorkingHours.parse("25:00-17:00,MON")).isEqualTo(WorkingHours.ALWAYS);
  }
}
