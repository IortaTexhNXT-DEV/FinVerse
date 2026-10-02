package com.iortatechnxt.brokerverse.eb.renewal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What a renewal advice announces and when its reminders are due (FR-EB-022 R1, R2). */
class RenewalTargetTest {

  private static EbProgramme programme(LocalDate... ends) {
    EbProgramme p =
        new EbProgramme(
            1L,
            "EBP-2026-000001",
            new EbProgramme.ClientRef(1L, "CL-1", "Client"),
            new EbProgramme.Profile("P", "BDO", EbFunding.EMPLOYER, "ebao", null, true));
    for (LocalDate end : ends) {
      p.addLine(new EbProgrammeLine.Data("HMO", null, null, null, null, null, end, 1));
    }
    return p;
  }

  @Test
  void theEarliestExpiryInTheWindowIsAnnounced() {
    LocalDate today = LocalDate.of(2026, 9, 1);
    EbProgramme p =
        programme(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 1, 10), LocalDate.of(2026, 12, 1));
    assertThat(RenewalTarget.of(p, today, null))
        .contains(new RenewalTarget(LocalDate.of(2026, 12, 1), 2026));
    assertThat(RenewalTarget.of(p, today, LocalDate.of(2026, 11, 30))).isEmpty();
    assertThat(RenewalTarget.of(programme((LocalDate) null), today, null)).isEmpty();
    EbProgramme removed = programme(LocalDate.of(2026, 10, 1));
    removed.line(1).deactivate();
    assertThat(RenewalTarget.of(removed, today, null)).isEmpty();
  }

  @Test
  void remindersAreDueOncePerReminderDayReached() {
    LocalDate expiry = LocalDate.of(2027, 1, 1);
    List<Integer> days = List.of(120, 105, 90);
    assertThat(RenewalTarget.remindersDue(expiry, days, expiry.minusDays(121))).isZero();
    assertThat(RenewalTarget.remindersDue(expiry, days, expiry.minusDays(120))).isEqualTo(1);
    assertThat(RenewalTarget.remindersDue(expiry, days, expiry.minusDays(100))).isEqualTo(2);
    assertThat(RenewalTarget.remindersDue(expiry, days, expiry)).isEqualTo(3);
  }
}
