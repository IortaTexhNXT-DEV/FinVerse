package com.iortatechnxt.finverse.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Gross written premium from the premium income statement lines.
 *
 * @param asOf reference date
 * @param yearStart first day of the fiscal year
 * @param monthToDate premium of the current month up to the reference date
 * @param monthToDatePriorYear same days one year earlier
 * @param yearToDate premium of the fiscal year up to the reference date
 * @param yearToDatePriorYear same period one year earlier
 * @param monthly current year against prior year per month
 */
public record PremiumWidget(
    LocalDate asOf,
    LocalDate yearStart,
    BigDecimal monthToDate,
    BigDecimal monthToDatePriorYear,
    BigDecimal yearToDate,
    BigDecimal yearToDatePriorYear,
    List<TrendPoint> monthly) {

  /** Canonical constructor copying the list. */
  public PremiumWidget {
    monthly = List.copyOf(monthly);
  }
}
