package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.domain.RelativePeriod;
import com.iortatechnxt.brokerverse.report.domain.ReportSubscription;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * When a report subscription runs and with which dates (G04): the working days of the company of
 * its parameters (holiday calendar of the head office; Monday to Friday without a company or head
 * office) for the last working day of the month, the time of day of the run, and the date
 * parameters set from its relative period on the business date of the run.
 */
@Component
public class SubscriptionCalendar {

  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the calendar.
   *
   * @param organization branches and holidays
   * @param clock clock
   */
  public SubscriptionCalendar(OrganizationService organization, Clock clock) {
    this.organization = organization;
    this.clock = clock;
  }

  /**
   * Whether a subscription runs now for a business date: due on the date and, on today's date, once
   * its time of day has come.
   *
   * @param s subscription
   * @param metadata its report
   * @param parameters its parameters
   * @param date business date of the run
   * @return true when it runs
   */
  public boolean runsNow(
      ReportSubscription s,
      ReportMetadata metadata,
      Map<String, String> parameters,
      LocalDate date) {
    if (!s.isDueOn(date, workingDays(metadata, parameters))) {
      return false;
    }
    return !date.equals(BusinessClock.today(clock))
        || s.isTimeReached(BusinessClock.now(clock).toLocalTime());
  }

  /**
   * The parameters of a run: the date parameters set from the relative period of the subscription
   * (a parameter whose name says "from" or "start" takes the first day, the others the last day);
   * unchanged without a period.
   *
   * @param period relative period, may be null
   * @param metadata report
   * @param parameters stored parameters
   * @param date business date of the run
   * @return parameters of the run
   */
  public static Map<String, String> withPeriod(
      RelativePeriod period,
      ReportMetadata metadata,
      Map<String, String> parameters,
      LocalDate date) {
    if (period == null) {
      return parameters;
    }
    RelativePeriod.Range range = period.on(date);
    Map<String, String> resolved = new HashMap<>(parameters);
    for (ParameterSpec spec : metadata.parameters()) {
      if (spec.type() == ParameterType.DATE) {
        String name = spec.name().toLowerCase(Locale.ROOT);
        boolean start = name.contains("from") || name.contains("start");
        resolved.put(spec.name(), (start ? range.from() : range.to()).toString());
      }
    }
    return resolved;
  }

  private Predicate<LocalDate> workingDays(
      ReportMetadata metadata, Map<String, String> parameters) {
    Optional<Long> company =
        metadata.parameters().stream()
            .filter(p -> p.type() == ParameterType.COMPANY)
            .map(p -> parameters.get(p.name()))
            .filter(v -> v != null && !v.isBlank())
            .map(v -> Long.valueOf(v.strip()))
            .findFirst();
    List<Branch> branches = company.map(organization::listBranches).orElse(List.of());
    return branches.stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
        .orElse(SubscriptionCalendar::weekday);
  }

  private static boolean weekday(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }
}
