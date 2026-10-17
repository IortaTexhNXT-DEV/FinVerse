package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * The insurer feedback due date of a commission billing (CMRID.011, OQ40): {@code
 * CMR_FEEDBACK_WORKING_DAYS} (10) working days after the sending, on the head office calendar
 * (weekly holidays and declared holidays); weekends only when the company has no head office.
 */
@Component
public class FeedbackCalendar {

  /** Working days allowed when the parameter is missing. */
  public static final int DEFAULT_DAYS = 10;

  private final OrganizationService organization;
  private final SystemParameterService parameters;

  /**
   * Creates the calendar.
   *
   * @param organization branches and holidays
   * @param parameters business parameters
   */
  public FeedbackCalendar(OrganizationService organization, SystemParameterService parameters) {
    this.organization = organization;
    this.parameters = parameters;
  }

  /**
   * The due date of feedback asked on a date.
   *
   * @param companyId company
   * @param sentOn date sent
   * @return due date
   */
  public LocalDate dueDate(Long companyId, LocalDate sentOn) {
    int days = parameters.intValue("CMR_FEEDBACK_WORKING_DAYS", DEFAULT_DAYS);
    Optional<Branch> office =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    Predicate<LocalDate> working =
        office
            .<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
            .orElse(FeedbackCalendar::weekday);
    LocalDate date = sentOn;
    int counted = 0;
    while (counted < days) {
      date = date.plusDays(1);
      if (working.test(date)) {
        counted++;
      }
    }
    return date;
  }

  private static boolean weekday(LocalDate date) {
    return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
  }
}
