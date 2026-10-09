package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.organization.domain.Holiday;
import com.iortatechnxt.brokerverse.organization.domain.HolidayRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facts the validations of a creation record read: the booked currency of each account
 * (FRS.CSH.02.01.08) and the working days of the holiday calendar maintained by Comptrollership
 * (holding period of checks).
 */
@Component
@Transactional(readOnly = true)
public class RecordFacts {

  private static final int CALENDAR_WINDOW = 60;

  private final PaymentMatcher matcher;
  private final HolidayRepository holidays;
  private final Clock clock;

  /**
   * Creates the facts.
   *
   * @param matcher account look-up
   * @param holidays holiday calendar
   * @param clock clock
   */
  public RecordFacts(PaymentMatcher matcher, HolidayRepository holidays, Clock clock) {
    this.matcher = matcher;
    this.holidays = holidays;
    this.clock = clock;
  }

  /**
   * The booked currency of each account of a record; unknown accounts are left out.
   *
   * @param companyId company
   * @param accounts accounts
   * @return currency per reference
   */
  public Map<String, String> currencies(Long companyId, List<RecordAccount> accounts) {
    Map<String, String> currencies = new HashMap<>();
    for (RecordAccount account : accounts) {
      List<OpsInvoice> invoices = matcher.invoices(companyId, account.reference());
      if (!invoices.isEmpty()) {
        currencies.put(account.reference(), invoices.get(0).getCurrency());
      }
    }
    return currencies;
  }

  /**
   * The date a given number of working days before today, on the company holiday calendar.
   *
   * @param companyId company
   * @param days working days
   * @return date
   */
  public LocalDate workingDaysBefore(Long companyId, int days) {
    LocalDate today = BusinessClock.today(clock);
    Set<LocalDate> off =
        holidays
            .findByCompanyIdAndHolidayDateBetweenOrderByHolidayDate(
                companyId, today.minusDays(CALENDAR_WINDOW), today)
            .stream()
            .map(Holiday::getHolidayDate)
            .collect(Collectors.toSet());
    return workingDaysBefore(today, days, off);
  }

  /**
   * The date a given number of working days before a day (Saturdays, Sundays and holidays are not
   * working days).
   *
   * @param day day
   * @param days working days
   * @param off holidays
   * @return date
   */
  static LocalDate workingDaysBefore(LocalDate day, int days, Set<LocalDate> off) {
    LocalDate date = day;
    int left = days;
    while (left > 0) {
      date = date.minusDays(1);
      boolean weekend =
          date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
      if (!weekend && !off.contains(date)) {
        left--;
      }
    }
    return date;
  }
}
