package com.iortatechnxt.brokerverse.renewal.dashboard.service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * The filters of the Renewal and Processing dashboards (BDOI Renewal FRS FRRN.002.02, FRRN.003.01):
 * the period, the market segment and the Account Officer; the processing dashboard adds the
 * business type (New Business, Renewal or both).
 *
 * @param companyId company
 * @param from first day of the period
 * @param to last day of the period
 * @param segment market segment or null for all
 * @param officer Account Officer (user name) or null for all
 * @param businessType NEW_BUSINESS, RENEWAL or null for both (processing dashboard)
 */
public record DashboardFilter(
    Long companyId,
    LocalDate from,
    LocalDate to,
    String segment,
    String officer,
    String businessType) {

  /** Blank filters are no filter; a missing period is the current month. */
  public DashboardFilter {
    segment = blank(segment) ? null : segment.strip();
    officer = blank(officer) ? null : officer.strip();
    businessType = blank(businessType) || "ALL".equals(businessType) ? null : businessType;
  }

  /**
   * The filter with the period completed: the current month when none is given, and the period
   * ordered.
   *
   * @param today business date
   * @return the filter
   */
  public DashboardFilter completed(LocalDate today) {
    LocalDate f = from == null ? YearMonth.from(today).atDay(1) : from;
    LocalDate t = to == null ? YearMonth.from(today).atEndOfMonth() : to;
    if (t.isBefore(f)) {
      LocalDate x = f;
      f = t;
      t = x;
    }
    return new DashboardFilter(companyId, f, t, segment, officer, businessType);
  }

  /**
   * The same filter one year earlier (growth, FRRN.002.02.01).
   *
   * @return the filter of the previous year
   */
  public DashboardFilter previousYear() {
    return new DashboardFilter(
        companyId, from.minusYears(1), to.minusYears(1), segment, officer, businessType);
  }

  /**
   * Whether a date falls in the period.
   *
   * @param date date or null
   * @return true when in the period
   */
  public boolean inPeriod(LocalDate date) {
    return date != null && !date.isBefore(from) && !date.isAfter(to);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}
