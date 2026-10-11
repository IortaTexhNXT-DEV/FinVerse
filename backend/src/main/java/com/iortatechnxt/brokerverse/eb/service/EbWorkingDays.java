package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Working days of the EB turn-around times and follow-ups (TAT annex; BRID-030): the head office
 * calendar of the company (weekends and holidays), or Monday to Friday when the company has no head
 * office.
 */
@Component
public class EbWorkingDays {

  private final OrganizationService organization;

  /**
   * Creates the calendar.
   *
   * @param organization branches and holidays
   */
  public EbWorkingDays(OrganizationService organization) {
    this.organization = organization;
  }

  /**
   * The date a number of working days after a date.
   *
   * @param companyId company
   * @param from start date (not counted)
   * @param days working days to add, 0 or more
   * @return the date
   */
  public LocalDate plus(Long companyId, LocalDate from, int days) {
    return plus(from, days, calendar(companyId));
  }

  /**
   * The date a number of working days after a date, on a given calendar.
   *
   * @param from start date (not counted)
   * @param days working days to add, 0 or more
   * @param working whether a date is a working day
   * @return the date
   */
  public static LocalDate plus(LocalDate from, int days, Predicate<LocalDate> working) {
    LocalDate date = from;
    int left = days;
    while (left > 0) {
      date = date.plusDays(1);
      if (working.test(date)) {
        left--;
      }
    }
    return date;
  }

  /**
   * The working-day calendar of a company.
   *
   * @param companyId company
   * @return whether a date is a working day
   */
  public Predicate<LocalDate> calendar(Long companyId) {
    Optional<Branch> head =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    return head.<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
        .orElse(EbWorkingDays::weekday);
  }

  private static boolean weekday(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }
}
