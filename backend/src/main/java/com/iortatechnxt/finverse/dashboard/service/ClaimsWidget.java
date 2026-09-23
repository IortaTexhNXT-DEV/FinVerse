package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Claims paid (claims expense accounts) and outstanding (claims reserve accounts).
 *
 * @param asOf reference date
 * @param paidMonthToDate claims paid in the current month
 * @param paidYearToDate claims paid in the fiscal year
 * @param outstanding outstanding claims reserve at the reference date
 * @param monthly claims paid per month against the prior year
 */
public record ClaimsWidget(
    LocalDate asOf,
    BigDecimal paidMonthToDate,
    BigDecimal paidYearToDate,
    BigDecimal outstanding,
    List<TrendPoint> monthly) {

  /** Canonical constructor copying the list. */
  public ClaimsWidget {
    monthly = List.copyOf(monthly);
  }
}
