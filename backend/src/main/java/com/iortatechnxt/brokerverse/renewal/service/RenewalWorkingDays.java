package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Business days of the Renewal and Processing dashboards (ageing of FRRN.003.02.01, the placement
 * turnaround time of FRRN.29.05): the head office calendar of the company (weekends and holidays),
 * or Monday to Friday when the company has no head office.
 */
@Component
public class RenewalWorkingDays {

  private final OrganizationService organization;

  /**
   * Creates the calendar.
   *
   * @param organization branches and holidays
   */
  public RenewalWorkingDays(OrganizationService organization) {
    this.organization = organization;
  }

  /**
   * The working-day calendar of a company.
   *
   * @param companyId company
   * @return whether a date is a business day
   */
  public Predicate<LocalDate> calendar(Long companyId) {
    Optional<Branch> head =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    return head.<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
        .orElse(RenewalWorkingDays::weekday);
  }

  /**
   * The business days after a start date up to an end date: the days of (start, end] that are
   * business days. A placement submitted on a Friday has an ageing of 1 on the following Monday.
   *
   * @param start start date (not counted)
   * @param end end date (counted)
   * @param working calendar
   * @return business days, 0 when the end is not after the start
   */
  public static int between(LocalDate start, LocalDate end, Predicate<LocalDate> working) {
    if (start == null || end == null || !end.isAfter(start)) {
      return 0;
    }
    int days = 0;
    for (LocalDate d = start.plusDays(1); !d.isAfter(end); d = d.plusDays(1)) {
      if (working.test(d)) {
        days++;
      }
    }
    return days;
  }

  /**
   * Monday to Friday.
   *
   * @param d date
   * @return true on a weekday
   */
  public static boolean weekday(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }
}
