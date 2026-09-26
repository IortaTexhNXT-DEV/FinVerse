package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.alert.service.AlertCheck.AlertSignal;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.service.CalendarEntry;
import com.iortatechnxt.brokerverse.tax.service.TaxCalendarService;
import com.iortatechnxt.brokerverse.tax.service.TaxFilingAlertCheck;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Filing calendar due states and the due / overdue alerts raised from it. */
@IntegrationTest
class TaxCalendarIT {

  private static final LocalDate AS_OF = LocalDate.of(2026, 9, 23);

  @Autowired private TaxCalendarService calendar;
  @Autowired private TaxFilingAlertCheck check;
  @Autowired private AlertService alerts;
  @Autowired private TaxFixtures fixtures;

  @BeforeEach
  void masters() {
    fixtures.masters();
  }

  @Test
  void calendarListsEveryFormPeriodWithItsDueState() {
    List<CalendarEntry> entries = calendar.calendar(fixtures.companyId(), 2026, AS_OF);
    assertThat(entries).extracting(CalendarEntry::formCode).contains("2550Q", "0619-E", "1601-C");
    CalendarEntry vatQ4 = entry(entries, "2550Q", "2026-Q4");
    assertThat(vatQ4.dueDate()).isEqualTo(LocalDate.of(2027, 1, 25));
    assertThat(vatQ4.dueState()).isEqualTo(CalendarEntry.UPCOMING);
    CalendarEntry dstSeptember = entry(entries, "2000", "2026-09");
    assertThat(dstSeptember.dueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(dstSeptember.daysToDue()).isEqualTo(12);
    assertThat(entry(entries, "1601-C", "2026-01").dueState()).isEqualTo(CalendarEntry.REMINDER);
    assertThat(entries)
        .filteredOn(e -> e.formCode().equals("0619-E"))
        .extracting(e -> e.period().label())
        .doesNotContain("2026-03", "2026-06");
  }

  @Test
  void dueAndOverdueReturnsRaiseAlertsOnce() {
    List<AlertSignal> signals = check.evaluate(LocalDate.of(2026, 10, 15));
    assertThat(signals)
        .extracting(AlertSignal::code)
        .contains(TaxCalendarService.OVERDUE_CODE, TaxCalendarService.DUE_CODE);
    assertThat(signals)
        .filteredOn(s -> s.code().equals(TaxCalendarService.DUE_CODE))
        .extracting(s -> s.facts().message())
        .anyMatch(m -> m.contains("falls due on 2026-10-"));
    assertThat(signals)
        .filteredOn(s -> s.code().equals(TaxCalendarService.OVERDUE_CODE))
        .extracting(s -> s.facts().message())
        .anyMatch(m -> m.startsWith("Tax return 0619-E 2026-08 was due on 2026-09-10"));
    AlertSignal first = signals.get(0);
    alerts.raise(first.code(), first.facts());
    assertThat(alerts.raise(first.code(), first.facts())).isEmpty();
    assertThat(calendar.dueSoonDays()).isEqualTo(15);
  }

  private static CalendarEntry entry(List<CalendarEntry> entries, String form, String label) {
    return entries.stream()
        .filter(e -> e.formCode().equals(form) && e.period().label().equals(label))
        .findFirst()
        .orElseThrow();
  }
}
