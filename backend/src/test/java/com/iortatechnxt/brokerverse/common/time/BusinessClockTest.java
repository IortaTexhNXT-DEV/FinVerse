package com.iortatechnxt.brokerverse.common.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The business date is the Manila date, also when the injected clock runs in UTC. */
class BusinessClockTest {

  /** 17:00 UTC on 30 June is 01:00 on 1 July in Manila. */
  private static final Clock EVENING_UTC =
      Clock.fixed(Instant.parse("2026-06-30T17:00:00Z"), ZoneOffset.UTC);

  /** 17:00 UTC on 31 December is 01:00 on 1 January in Manila. */
  private static final Clock NEW_YEAR_EVE_UTC =
      Clock.fixed(Instant.parse("2026-12-31T17:00:00Z"), ZoneOffset.UTC);

  @Test
  void defaultZoneIsManila() {
    assertThat(BusinessClock.zone()).isEqualTo(ZoneId.of("Asia/Manila"));
    assertThat(BusinessClock.zoneId()).isEqualTo("Asia/Manila");
  }

  @Test
  void todayIsTheManilaDateAt1700Utc() {
    assertThat(LocalDate.ofInstant(EVENING_UTC.instant(), ZoneOffset.UTC))
        .isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(BusinessClock.today(EVENING_UTC)).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(BusinessClock.currentMonth(EVENING_UTC)).isEqualTo(YearMonth.of(2026, 7));
  }

  @Test
  void yearTurnsAtManilaMidnight() {
    assertThat(BusinessClock.today(NEW_YEAR_EVE_UTC)).isEqualTo(LocalDate.of(2027, 1, 1));
    assertThat(BusinessClock.currentYear(NEW_YEAR_EVE_UTC)).isEqualTo(Year.of(2027));
  }

  @Test
  void morningUtcIsTheSameDay() {
    Clock morning = Clock.fixed(Instant.parse("2026-06-30T03:00:00Z"), ZoneOffset.UTC);
    assertThat(BusinessClock.today(morning)).isEqualTo(LocalDate.of(2026, 6, 30));
  }

  @Test
  void recordedInstantsFallOnTheManilaDay() {
    assertThat(BusinessClock.dateOf(EVENING_UTC.instant())).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(BusinessClock.dateOf(null)).isNull();
    assertThat(BusinessClock.startOf(LocalDate.of(2026, 7, 1)))
        .isEqualTo(Instant.parse("2026-06-30T16:00:00Z"));
  }

  @Test
  void zoneOfTheInjectedClockIsIgnored() {
    Clock honolulu = EVENING_UTC.withZone(ZoneId.of("Pacific/Honolulu"));
    assertThat(BusinessClock.today(honolulu)).isEqualTo(LocalDate.of(2026, 7, 1));
  }

  @Test
  void configuredZoneIsRead() {
    assertThat(BusinessZoneSettings.resolve(null)).isEqualTo(ZoneId.of("Asia/Manila"));
    assertThat(BusinessZoneSettings.resolve(" ")).isEqualTo(ZoneId.of("Asia/Manila"));
    assertThat(BusinessZoneSettings.resolve("Asia/Singapore"))
        .isEqualTo(ZoneId.of("Asia/Singapore"));
    assertThatThrownBy(() -> BusinessZoneSettings.resolve("Mars/Olympus"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("brokerverse.business-zone");
  }
}
