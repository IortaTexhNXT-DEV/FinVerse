package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Claim ages and ageing buckets (BRCLM.025/027, FR-CL-053) and the workflow actions per phase. */
class ClaimAgeingTest {

  private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
  private static final List<Integer> BOUNDS = List.of(30, 60, 90, 180);

  @Test
  void ageOverallRunsFromTheReportedDateToTodayOrTheClosure() {
    assertThat(ClaimAgeing.ageOverall(MARCH_1, null, LocalDate.of(2026, 3, 31))).isEqualTo(30);
    assertThat(
            ClaimAgeing.ageOverall(MARCH_1, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 31)))
        .isEqualTo(14);
    assertThat(ClaimAgeing.ageOverall(MARCH_1, null, MARCH_1.minusDays(1))).isZero();
  }

  @Test
  void ageThisStageCountsFromTheStatusDateInManila() {
    Instant lateOnFeb28Utc = Instant.parse("2026-02-28T17:00:00Z"); // 1 March 01:00 in Manila
    assertThat(ClaimAgeing.ageThisStage(lateOnFeb28Utc, LocalDate.of(2026, 3, 3))).isEqualTo(2);
    assertThat(ClaimAgeing.ageThisStage(null, MARCH_1)).isZero();
    assertThat(ClaimAgeing.dateOf(null)).isNull();
    Clock clock = Clock.fixed(lateOnFeb28Utc, ZoneOffset.UTC);
    assertThat(ClaimAgeing.today(clock)).isEqualTo(MARCH_1);
  }

  @Test
  void bucketsFollowTheParameter() {
    assertThat(ClaimAgeing.bucket(0, BOUNDS)).isEqualTo("0-30");
    assertThat(ClaimAgeing.bucket(45, BOUNDS)).isEqualTo("31-60");
    assertThat(ClaimAgeing.bucket(90, BOUNDS)).isEqualTo("61-90");
    assertThat(ClaimAgeing.bucket(95, BOUNDS)).isEqualTo("91-180");
    assertThat(ClaimAgeing.bucket(181, BOUNDS)).isEqualTo("181+");
    assertThat(ClaimAgeing.buckets(BOUNDS))
        .containsExactly("0-30", "31-60", "61-90", "91-180", "181+");
    assertThat(ClaimAgeing.buckets(List.of())).containsExactly("0+");
  }

  @Test
  void everyPhaseChangeHasItsWorkflowAction() {
    assertThat(ClaimWorkflow.action(ClaimPhase.NEW, ClaimPhase.IN_PROGRESS)).isEqualTo("progress");
    assertThat(ClaimWorkflow.action(ClaimPhase.IN_PROGRESS, ClaimPhase.TEMP_CLOSED))
        .isEqualTo("temp_close");
    assertThat(ClaimWorkflow.action(ClaimPhase.TEMP_CLOSED, ClaimPhase.IN_PROGRESS))
        .isEqualTo("resume");
    assertThat(ClaimWorkflow.action(ClaimPhase.NEW, ClaimPhase.CLOSED)).isEqualTo("close");
    assertThat(ClaimWorkflow.action(ClaimPhase.CLOSED, ClaimPhase.IN_PROGRESS)).isEqualTo("reopen");
    assertThatThrownBy(() -> ClaimWorkflow.action(ClaimPhase.IN_PROGRESS, ClaimPhase.NEW))
        .isInstanceOf(IllegalStateException.class);
  }
}
