package com.iortatechnxt.brokerverse.closing.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.period.domain.FiscalYear;
import com.iortatechnxt.brokerverse.period.domain.FiscalYearStatus;
import com.iortatechnxt.brokerverse.period.service.PeriodService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Daily check {@code YEAR_END_CLOSE_DUE} (FRBS 2.7.0): the previous fiscal year must be closed on
 * or before {@code YEAR_END_CLOSE_DEADLINE} (15 April); from the alert's threshold days before the
 * deadline (15 by default) an open previous year raises the alert, once per company and year.
 */
@Component
public class YearEndCloseDueCheck implements AlertCheck {

  /** Alert code. */
  public static final String CODE = "YEAR_END_CLOSE_DUE";

  private static final MonthDay DEFAULT_DEADLINE = MonthDay.of(4, 15);
  private static final int DEFAULT_DAYS = 15;

  private final OrganizationService organization;
  private final PeriodService periods;
  private final SystemParameterService parameters;
  private final AlertService alerts;

  /**
   * Creates the check.
   *
   * @param organization companies
   * @param periods fiscal years
   * @param parameters deadline parameter
   * @param alerts alert settings (threshold days)
   */
  public YearEndCloseDueCheck(
      OrganizationService organization,
      PeriodService periods,
      SystemParameterService parameters,
      AlertService alerts) {
    this.organization = organization;
    this.periods = periods;
    this.parameters = parameters;
    this.alerts = alerts;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    LocalDate deadline = deadline().atYear(asOf.getYear());
    int days = alerts.activeCode(CODE).map(ExceptionCode::getThresholdDays).orElse(DEFAULT_DAYS);
    if (asOf.isBefore(deadline.minusDays(days))) {
      return List.of();
    }
    return organization.listCompanies().stream()
        .map(c -> signal(c, asOf, deadline))
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * The deadline month and day ({@code YEAR_END_CLOSE_DEADLINE}, MM-DD).
   *
   * @return deadline, 15 April when the parameter is missing or invalid
   */
  public MonthDay deadline() {
    String text = parameters.text("YEAR_END_CLOSE_DEADLINE", "04-15").trim();
    try {
      return MonthDay.parse("--" + text);
    } catch (DateTimeException ex) {
      return DEFAULT_DEADLINE;
    }
  }

  private Optional<AlertSignal> signal(Company company, LocalDate asOf, LocalDate deadline) {
    return periods.listYears(company.getId()).stream()
        .filter(y -> y.getEndDate().isBefore(asOf))
        .max(Comparator.comparing(FiscalYear::getEndDate))
        .filter(y -> y.getStatus() != FiscalYearStatus.CLOSED)
        .map(
            y ->
                new AlertSignal(
                    CODE,
                    new AlertFacts(
                        company.getId(),
                        null,
                        "FiscalYear",
                        String.valueOf(y.getYearCode()),
                        "Fiscal year "
                            + y.getYearCode()
                            + " of "
                            + company.getCode()
                            + " is not closed; the deadline is "
                            + deadline,
                        null,
                        CODE + ":" + company.getId() + ":" + y.getYearCode())));
  }
}
