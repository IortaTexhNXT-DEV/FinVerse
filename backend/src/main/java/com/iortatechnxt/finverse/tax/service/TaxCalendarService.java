package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.alert.service.AlertService;
import com.iortatechnxt.finverse.tax.domain.ReturnStatus;
import com.iortatechnxt.finverse.tax.domain.TaxForm;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.TaxReturn;
import com.iortatechnxt.finverse.tax.domain.TaxReturnRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tax filing calendar: every period of every authorized form in a year (from the form's
 * effective date), with its due date, the live return and a due state relative to a reference date.
 * "Due soon" uses the threshold days of exception code {@code TAX_RETURN_DUE} (default 15).
 */
@Service
@Transactional(readOnly = true)
public class TaxCalendarService {

  /** Exception code for returns falling due. */
  public static final String DUE_CODE = "TAX_RETURN_DUE";

  /** Exception code for overdue returns. */
  public static final String OVERDUE_CODE = "TAX_RETURN_OVERDUE";

  private static final int DEFAULT_DUE_DAYS = 15;

  private final TaxFormService forms;
  private final TaxReturnRepository returns;
  private final AlertService alerts;

  /**
   * Creates the service.
   *
   * @param forms tax forms
   * @param returns returns
   * @param alerts exception code thresholds
   */
  public TaxCalendarService(
      TaxFormService forms, TaxReturnRepository returns, AlertService alerts) {
    this.forms = forms;
    this.returns = returns;
    this.alerts = alerts;
  }

  /**
   * Calendar of a year.
   *
   * @param companyId company
   * @param year calendar year of the filing periods
   * @param asOf reference date for the due states
   * @return entries ordered by due date and form
   */
  public List<CalendarEntry> calendar(Long companyId, int year, LocalDate asOf) {
    int dueDays = dueSoonDays();
    Map<String, TaxReturn> live = new HashMap<>();
    returns
        .search(
            companyId,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 1, 1).plusYears(1).minusDays(1),
            null,
            null)
        .stream()
        .filter(r -> r.getStatus() != ReturnStatus.CANCELLED)
        .forEach(r -> live.put(key(r.getFormCode(), r.getPeriodStart()), r));
    List<CalendarEntry> out = new ArrayList<>();
    for (TaxForm form : forms.active(companyId)) {
      for (TaxPeriod period : form.schedule().periods(year)) {
        if (!period.to().isBefore(form.getEffectiveFrom())) {
          Optional<TaxReturn> r = Optional.ofNullable(live.get(key(form.getCode(), period.from())));
          out.add(entry(form, period, r, asOf, dueDays));
        }
      }
    }
    out.sort(Comparator.comparing(CalendarEntry::dueDate).thenComparing(CalendarEntry::formCode));
    return out;
  }

  /**
   * Threshold of the "due soon" state.
   *
   * @return days before the due date
   */
  public int dueSoonDays() {
    return alerts
        .activeCode(DUE_CODE)
        .map(c -> c.getThresholdDays() == null ? DEFAULT_DUE_DAYS : c.getThresholdDays())
        .orElse(DEFAULT_DUE_DAYS);
  }

  private static CalendarEntry entry(
      TaxForm form, TaxPeriod period, Optional<TaxReturn> r, LocalDate asOf, int dueDays) {
    LocalDate due = form.schedule().dueDate(period.to());
    long days = ChronoUnit.DAYS.between(asOf, due);
    String status = r.map(x -> x.getStatus().name()).orElse(CalendarEntry.NOT_PREPARED);
    return new CalendarEntry(
        form.getCode(),
        form.getName(),
        form.getAuthority(),
        form.getWorksheet(),
        period,
        due,
        form.isTrackFiling(),
        r.map(TaxReturn::getId).orElse(null),
        r.map(TaxReturn::getReturnNo).orElse(null),
        status,
        dueState(form.isTrackFiling(), status, days, dueDays),
        days);
  }

  private static String dueState(boolean tracked, String status, long days, int dueDays) {
    if (!tracked) {
      return CalendarEntry.REMINDER;
    }
    if (ReturnStatus.PAID.name().equals(status)) {
      return CalendarEntry.PAID;
    }
    if (days < 0) {
      return CalendarEntry.OVERDUE;
    }
    return days <= dueDays ? CalendarEntry.DUE_SOON : CalendarEntry.UPCOMING;
  }

  private static String key(String form, LocalDate start) {
    return form + "|" + start;
  }
}
