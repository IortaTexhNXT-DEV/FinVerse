package com.iortatechnxt.brokerverse.renewal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardItem;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Placement turnaround time (FRRN.29.05, FRRN.003.02.03): a placement sent at or after the 3 PM
 * cut-off counts from the next business day; a resolved placement issue stops the count.
 */
class PlacementTatTest {

  private static DashboardItem placed(LocalDateTime at) {
    Map<String, Object> m = new HashMap<>();
    m.put("kind", "RENEWAL");
    m.put("placed_at", at.atZone(BusinessClock.zone()).toInstant());
    return new DashboardItem(m);
  }

  @Test
  void aPackagedPlacementSentAt4PmOnMondayIsBeyondTatFromTheFourthBusinessDay() {
    DashboardItem i = placed(LocalDateTime.of(2026, 10, 5, 16, 0));
    LocalDate friday = LocalDate.of(2026, 10, 9);
    LocalDate monday = LocalDate.of(2026, 10, 12);
    int onFriday = ProcessingAgeing.of(i, friday);
    int onMonday = ProcessingAgeing.of(i, monday);
    assertThat(onFriday).isEqualTo(3);
    assertThat(onFriday > 3).isFalse();
    assertThat(onMonday).isEqualTo(4);
    assertThat(onMonday > 3).isTrue();
  }

  @Test
  void aPlacementSentBeforeTheCutOffCountsFromTheSameDay() {
    DashboardItem i = placed(LocalDateTime.of(2026, 10, 5, 10, 0));
    assertThat(ProcessingAgeing.of(i, LocalDate.of(2026, 10, 9))).isEqualTo(4);
  }

  @Test
  void theResolutionDateOfAnIssueEndsTheCount() {
    DashboardItem i = placed(LocalDateTime.of(2026, 10, 5, 10, 0));
    i.put("with_issue", true);
    i.put("resolution_date", LocalDate.of(2026, 10, 7));
    assertThat(ProcessingAgeing.of(i, LocalDate.of(2026, 10, 20))).isEqualTo(2);
  }

  /** Calls the ageing of the processing dashboard with a Monday-to-Friday calendar. */
  private static final class ProcessingAgeing {
    static int of(DashboardItem i, LocalDate today) {
      return com.iortatechnxt.brokerverse.renewal.dashboard.service.ProcessingDashboardService
          .placementAgeing(i, today, RenewalWorkingDays::weekday, 15);
    }
  }
}
