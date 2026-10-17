package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Daily check of the tax calendar (evaluated by the {@code ALERT_DAILY_CHECKS} job): raises {@code
 * TAX_RETURN_DUE} for tracked returns falling due within the threshold days and {@code
 * TAX_RETURN_OVERDUE} for tracked returns past their due date and not paid. The current and the
 * previous calendar year are checked, so a return of December is still watched in January. One
 * alert per form and period stays open until resolved (deduplication key).
 */
@Component
public class TaxFilingAlertCheck implements AlertCheck {

  private final TaxCalendarService calendar;
  private final OrganizationService organization;
  private final AlertService alerts;

  /**
   * Creates the check.
   *
   * @param calendar tax calendar
   * @param organization companies
   * @param alerts active exception codes
   */
  public TaxFilingAlertCheck(
      TaxCalendarService calendar, OrganizationService organization, AlertService alerts) {
    this.calendar = calendar;
    this.organization = organization;
    this.alerts = alerts;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    boolean due = alerts.activeCode(TaxCalendarService.DUE_CODE).isPresent();
    boolean overdue = alerts.activeCode(TaxCalendarService.OVERDUE_CODE).isPresent();
    List<AlertSignal> out = new ArrayList<>();
    if (!due && !overdue) {
      return out;
    }
    for (Company company : organization.listCompanies()) {
      for (int year = asOf.getYear() - 1; year <= asOf.getYear(); year++) {
        calendar.calendar(company.getId(), year, asOf).stream()
            .map(e -> toSignal(company.getId(), e, due, overdue))
            .flatMap(Optional::stream)
            .forEach(out::add);
      }
    }
    return out;
  }

  private static Optional<AlertSignal> toSignal(
      Long companyId, CalendarEntry e, boolean due, boolean overdue) {
    if (due && CalendarEntry.DUE_SOON.equals(e.dueState())) {
      return Optional.of(signal(TaxCalendarService.DUE_CODE, companyId, e, "falls due on "));
    }
    if (overdue && CalendarEntry.OVERDUE.equals(e.dueState())) {
      return Optional.of(signal(TaxCalendarService.OVERDUE_CODE, companyId, e, "was due on "));
    }
    return Optional.empty();
  }

  private static AlertSignal signal(String code, Long companyId, CalendarEntry e, String verb) {
    String subject = e.formCode() + " " + e.period().label();
    return new AlertSignal(
        code,
        new AlertFacts(
            companyId,
            null,
            "TaxReturn",
            e.returnNo() == null ? subject : e.returnNo(),
            "Tax return " + subject + " " + verb + e.dueDate() + " (" + e.returnStatus() + ")",
            null,
            code + ":" + companyId + ":" + e.formCode() + ":" + e.period().from()));
  }
}
