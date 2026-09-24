package com.iortatechnxt.brokerverse.dashboard.service;

import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reference date and fiscal year of dashboard widgets. A company without a fiscal year for the date
 * (new company, year not opened yet) falls back to the calendar year, so widgets show zeros instead
 * of failing.
 */
@Component
public class DashboardCalendar {

  private final PeriodService periods;
  private final Clock clock;

  /**
   * Creates the calendar.
   *
   * @param periods fiscal years
   * @param clock clock
   */
  public DashboardCalendar(PeriodService periods, Clock clock) {
    this.periods = periods;
    this.clock = clock;
  }

  /**
   * The requested reference date, or today.
   *
   * @param asOf requested date, may be null
   * @return reference date
   */
  public LocalDate dateOrToday(LocalDate asOf) {
    return asOf != null ? asOf : LocalDate.now(clock);
  }

  /**
   * First day of the fiscal year containing a date.
   *
   * @param companyId company
   * @param date date
   * @return fiscal year start, else 1 January of the date's year
   */
  @Transactional(readOnly = true)
  public LocalDate yearStart(Long companyId, LocalDate date) {
    return fiscalYear(companyId, date).map(FiscalYear::getStartDate).orElse(date.withDayOfYear(1));
  }

  /**
   * The fiscal year containing a date.
   *
   * @param companyId company
   * @param date date
   * @return fiscal year, empty when none is defined for the date
   */
  @Transactional(readOnly = true)
  public Optional<FiscalYear> fiscalYear(Long companyId, LocalDate date) {
    return periods.listYears(companyId).stream()
        .filter(y -> !date.isBefore(y.getStartDate()) && !date.isAfter(y.getEndDate()))
        .findFirst();
  }
}
