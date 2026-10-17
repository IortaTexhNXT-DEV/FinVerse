package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Working days of the case turnaround times (FR-CSF-051 R2; CLR-CSF-09): the holiday calendar of
 * the head office of the company, or Monday to Friday when the company has no head office.
 */
@Component
public class CsfCaseCalendar {

  private final OrganizationService organization;

  /**
   * Creates the calendar.
   *
   * @param organization branches and holidays
   */
  public CsfCaseCalendar(OrganizationService organization) {
    this.organization = organization;
  }

  /**
   * The working-day test of a company.
   *
   * @param companyId company
   * @return whether a date is a working day
   */
  public Predicate<LocalDate> of(Long companyId) {
    Optional<Branch> head =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    return head.<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
        .orElse(CsfCaseCalendar::weekday);
  }

  /**
   * The date a number of working days after a start date.
   *
   * @param start start date (not counted)
   * @param days working days
   * @param working calendar
   * @return date
   */
  public static LocalDate plus(LocalDate start, int days, Predicate<LocalDate> working) {
    LocalDate d = start;
    int left = days;
    while (left > 0) {
      d = d.plusDays(1);
      if (working.test(d)) {
        left--;
      }
    }
    return d;
  }

  /**
   * The working days after a start date up to an end date.
   *
   * @param start start date (not counted)
   * @param end end date (counted)
   * @param working calendar
   * @return working days, 0 when the end is not after the start
   */
  public static int between(LocalDate start, LocalDate end, Predicate<LocalDate> working) {
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
